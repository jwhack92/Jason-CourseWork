//Jason Hack

#include "mmu.h"
#include <stdio.h>

/* I'm defining the CR3 "register" as a void*, but you are free to redefine it
 * as necessary.  Keep in mind that whatever type you use, it MUST be a
 * pointer.  However, the linker will not verify this.  If you redefine this
 * global as some non-pointer type, your program will probably crash.
 */
extern void* CR3;

/* The supervisor mode indicates that the processor is running in the operating
 * system.  If the permission bits are used, e.g., not in legacy mode, then
 * accessing a privileged page is only valid when SUPER is true.  Otherwise, it
 * is a protection fault.
 */
extern int  SUPER;


/* The page table is the same for both 16-bit and 32-bit addressing modes:
 *
 *  31    30..28 27..............................................4 3 2 1 0
 * +-----+------+-------------------------------------------------+-+-+-+-+
 * |Valid|unused| 24-bit Physical Page Number                     |P|R|W|X|
 * +-----+------+-------------------------------------------------+-+-+-+-+
 *
 * Unlike Intel, which uses a 4k (12-bit) page, this system uses a much smaller,
 * 256-byte (8-bit) page.
 */

/* 16-bit legacy mode.
 * In legacy mode, CR3 points to an array of PTE.  Since there are only 256
 * pages, this array is rather small (1k).  Legacy mode doesn't enforce
 * permissions; every page is assumed to be read-write-executed regardless of
 * the permission bits.
 */

// returns a reslt_t object designating a sucessful result_t
result_t success(unsigned pa) {
  result_t r;
  r.status = SUCCESS;
  r.value.pa = (void*) pa;
  return r;
}

// returns a result_t object designating a protection fault
result_t protectionfault(unsigned pa) {
  result_t r;
  r.status = PROTFAULT;
  r.value.pa = (void*) pa;
  return r;
}

// returns a result_t object designating a pagefault
result_t pagefault(unsigned pa) {
  result_t r;
  r.status = PAGEFAULT;
  r.value.pa = (void*) pa;
  return r;
}

// determines whether or not a pte is valid by examining the valid bit
int pte_valid(unsigned pte) {
  return 1 & (pte >> 31);

}

// given a pte, returns the corresponding physical page number
unsigned pte_ppn(unsigned pte) {
  return 0xFFFFFF & (pte >> 4);
}

// implements 16 bit addressing mode
result_t mmu_legacy(unsigned short va)
{
  unsigned vpn = 0xFF & (va >> 8);
  unsigned po = 0xFF & va;
  unsigned* cr3 = CR3;
  unsigned pte = cr3[vpn]; // indexes off of cr3 to give us our pte

  // if our pte is valid, we return the success state, else we have a pagefault
  if(pte_valid(pte)) {
    return success(((pte_ppn(pte)<<8) | po));
  }
    else{
      return pagefault(vpn);
    }
}

/* In 32-bit mode, CR3 points to an array of 256 directory pointers.  The format
 * of a directory pointer is the following:
 *
 *  31......................................................4 3....1     0
 * +---------------------------------------------------------+------+-----+
 * | Address of page table directory                         |Unused|Valid|
 * +---------------------------------------------------------+------+-----+
 *
 * Note that only 24-bits are needed for the pointer.  Each directory starts on
 * a page boundary, which means the four lest significant bits are always zero.
 * This is where we hide the valid bit.
 *
 * Each directory is an array of pointers with the same format above.  These
 * pointers, however, point at page tables.  Decoding a 32-bit address looks
 * like this:
 *
 *  31...........24 23..............16 15....................4 3.........0
 * +---------------+------------------+-----------------------+-----------+
 * |Directory Index| Page Table Index |Page Table Entry Index |Page Offset|
 * +---------------+------------------+-----------------------+-----------+
 *       |                  |                 |                     
 * CR3   |   Root Dir.      |   Directory     |    Page Table       
 *  |    |   +------+       |   +------+      |     +------+        
 *  |    |   |      |       |   |      |      |     |      |        
 *  |    |   |      |       |   |      |      |     |      |
 *  |    +-> |======|--+    +-> |======|--+   +---> |======|---> PPN
 *  |        |      |  |        |      |  |         |      |
 *  |        |      |  |        |      |  |         |      |
 *  |        |      |  |        |      |  |         |      |
 *  |        |      |  |        |      |  |         |      |
 *  +------> +------+  +------> +------+  +-------> +------+
 *
 * Of course, you must check the valid bit at each level.  To be clear, the
 * diagrams are "upside-down" (i.e., little address are at the bottom).  
 */

/* 32-bit mode with protection.
 */

// Checks the valid bit to conferm whether or not a pointer is valid
int valid_pointer(unsigned p) {
  return p & 1;
}

// returns the address of a given pointer
void* pointer_address(unsigned p) {
  return (void*) (p & (~1));
}

// This method determines whether or not a protection fault occurs.
// returns 1 if a protection fault occurs, 0 otherwise
int useFault(access_t use, unsigned pte) {
  int execute = pte & 1;
  int write = (pte >> 1) & 1;
  int read = (pte >> 2) & 1;
  int permission = (pte >> 3) & 1;

  // if result = 1 a protection fault occurs, if 0 a protection fault does not occur.
  int result = (use == READ && !read) || 
    (use == WRITE && !write) || 
    (use == EXECUTE && !execute);
  result = result || (permission && !SUPER);
  return result;
}

// If the pte we found is a valid pte, then we check if a protection fault occurs.
// If a protection fault does not occur, we report SUCCESS
result_t evaluateValidPte(access_t use, unsigned pte, unsigned po) {
  if(useFault(use, pte))
    return protectionfault(pte);
  else{
    return success(((pte_ppn(pte)<<8) | po));
  }
}

// This method implements 32 bit addressing mode. 
result_t mmu_resolve(void* va, access_t use)
{

  unsigned* cr3 = CR3;
  unsigned uva = (unsigned) va;
  unsigned tlb_index = (uva >> 8) & 0xFF;
  unsigned tlb_tag = (uva >> 16) & 0xFFFF;
  unsigned vpn = (uva >> 8) & 0xFFFFFF;
  unsigned  po = 0xFF & uva;
  unsigned  pteIndex = 0xFF & (uva >> 8);
  unsigned  ptIndex = 0xFF & (uva >> 16);
  unsigned  dirIndex = 0xFF & (uva >> 24);
  unsigned pte;
  // if we find the pte in the tlb, then we have a valid pte, and we evaluate the pte to 
  // determine whether or not a protection fault occurs. 
  if (tlb_search(tlb_index, tlb_tag, &pte)) {
    return evaluateValidPte(use, pte, po);
  }
  unsigned dirPointer =  cr3[dirIndex];
  // determines whether or not the pointer to the directory is valid.
  if(valid_pointer(dirPointer)) {
    unsigned* dirArray = pointer_address(dirPointer);
    unsigned ptPointer = dirArray[ptIndex];
    // determines whether or not the pointer to the page table is valid
    if(valid_pointer(ptPointer)) {
      unsigned* ptArray = pointer_address(ptPointer);
      pte = ptArray[pteIndex];
      // determines whether or not the pte is a valid pte
      if(pte_valid(pte)) {
	// add the valid pte to the tlb
	tlb_add(tlb_index, tlb_tag, pte);
	return evaluateValidPte(use, pte, po);
      }
    }
    return pagefault(vpn);
  }
}
