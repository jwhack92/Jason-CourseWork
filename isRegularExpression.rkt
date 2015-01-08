; This program determines if a given input is a regular expression.
; For the sake of this program, epsilon stands for ϵ, empty stands for ∅, o stands for the concatenation operator
; / stands for a left parenthesis and & stands for a right parenthesis
; epsilon, empty, and any atom other than o is considered a regular expression
; If R and S are regular expressions then / R &, R + S, R o S and R * are regular expressions.



; Jason Hack   W00979077
; Assignment2
; CSCI 301 22310
; 6/4/2014


#lang racket

(define list '())

;Given a list of symbols as input, determines whether or not the list is a legal regular expression
(define RE?
  (lambda (L)
    (set! list L)
        (cond ( (null? L) (error "#f \n Error: given list is empty"))
              (else 
               (let ([answer (S: L)])
                  (if (eq? answer "endParen") 
                        (error "#f \n Error: end parentheses without begin parentheses")
                        answer))))))

;The start state of the parser, it has the form
; S --> read atom ; E | read / ; S ; read &
(define S:
  (lambda (L)
    (cond ( (null? L) #f)
            ( (not-member? (car L) '(o / + & *)) 
              (begin
                (set! list (cdr list))
                (E: list)))
            ( (eq? (car L) '/) 
              (begin
                (set! list (cdr list))
                (if (eq? (S: list) "endParen") (E: list) (error "#f \n Error: begin parentheses without end parentheses"))
                
                ))
            (else (if (eq? (car list) '&) 
                      (error "#f \n Error: expected a regular expression, found end parentheses instead")
                      (error "#f \n Error: expected a regular expression, found operator instead"))) 
          )))

;The second/end state of my parser, it uses the form
; E --> read + ; S | read o ; S | read * ; E | read Λ
(define E:
  (lambda (L)
    (cond ( (null? L) #t)
          ( (or (eq? (car L) 'o) (eq? (car L) '+)) 
             (begin
               (set! list (cdr list))
               (S: list)))
          ( (eq? (car L) '*) 
            (begin
              (set! list (cdr list))
              (E: list)))
          ( (eq? (car L) '&) 
            (begin
              (set! list (cdr list))
              "endParen"))
          (else (error "#f \n Expected an operator, found regular expression instead")))))

;Helper method, given a token and a list, returns whether the token is not in the list
(define not-member?
  (lambda (a S)
    (cond ( (null? S) #t)
          ( (eq? a (car S)) #f)
          (else
           (not-member? a (cdr S))
           ))))
