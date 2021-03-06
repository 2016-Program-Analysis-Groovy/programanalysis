/*
 * ANTRL (http://www.antlr.org/) grammar for the project language. You will
 * probably want to adapt the file to generate parser for your language of
 * choice and use your own data structures (or define tree parser to traverse
 * the tree generated by ANTLR).
 *
 * Note that this has not been throughly tested, so let us know if there are
 * any problems.
 */
grammar MicroC;

  AND : '&';
  OR : '|';
  ASSIGN : '=';
  SEMI : ';';
  GT : '>';
  GE : '>=';
  LT : '<';
  LE : '<=';
  EQ : '==';
  NEQ : '!=';
  PLUS : '+';
  MINUS : '-';
  MUL : '*';
  DIV : '/';
  NOT : '!';
  LPAREN : '(';
  RPAREN : ')';
  LBRACE : '{';
  RBRACE : '}';
  LBRACKET : '[';
  RBRACKET : ']';
  COLON : ':';
  IF : 'if';
  ELSE : 'else';
  WHILE : 'while';
  CONTINUE : 'continue';
  BREAK : 'break';
  WRITE : 'write';
  READ : 'read';
  INT : 'int';
  VOID : 'void';


	  
aexpr : expr1
      | expr1 PLUS aexpr
      | expr1 MINUS aexpr;

expr1 : expr2
       | expr2 MUL expr1
       | expr2 DIV expr1 ;
	   
expr2 : exprnegate
       | identifier (LBRACKET expr RBRACKET)?
       | integer
       ;

expr : bexpr1
       | bexpr1 OR expr
       ;
	   
bexpr1 : bexpr2
        | bexpr2 AND bexpr1;
		
bexpr2 :aexpr opr aexpr
       |aexpr;
	
exprnegate:
           |MINUS expr
           | NOT expr;
       



opr : GT
    | GE
    | LT
    | LE
    | EQ
    | NEQ
    ;


decl : type identifier (LBRACKET integer RBRACKET)? SEMI ;

type : INT
     | VOID ;

stmt : assignStmt
     | continueStmt
	 | breakStmt
     | readStmt
     | writeStmt
	 | ifelseStmt
     | whileStmt
	 | blockStmt
     ;

assignStmt : identifier (LBRACKET expr RBRACKET)? ASSIGN expr SEMI ;

continueStmt : CONTINUE SEMI ;

readStmt : READ identifier (LBRACKET expr RBRACKET)? SEMI ;

breakStmt : BREAK SEMI ;

writeStmt : WRITE expr SEMI ;

ifelseStmt : IF LPAREN expr RPAREN  LBRACE stmt+ RBRACE (ELSE LBRACE stmt+ RBRACE)? ;


whileStmt : WHILE LPAREN expr RPAREN  LBRACE stmt+ RBRACE ;

blockStmt : LBRACE decl* stmt+ RBRACE ;

program :  LBRACE decl* stmt+ RBRACE ;


identifier : IDENTIFIER;

integer : INTEGER;

COMMENT : '/*' .*? '*/' -> channel(HIDDEN);
     

INTEGER : ('0' | '1'..'9' '0'..'9'*);

IDENTIFIER : LETTER (LETTER|'0'..'9')* ;

fragment
LETTER : 'A'..'Z'
       | 'a'..'z'
       | '_'
       ;

WS : (' '|'\r'|'\t'|'\u000C'|'\n') -> skip ;
