grammar IdentityCredentialQueryLanguage;

query : binexpr ;

binexpr : binref
     | binexpr ( ('&&' | '||') binexpr )+
     | relexpr
     | '(' binexpr ')';

relexpr : ref OPERATOR ref ;

binref :  namespace '/' ID '#b' |  ID '#b';

ref : dataref | paramref ;

dataref : namespace '/' ID ('#s' | '#i');

paramref : ID ('#s' | '#i');

namespace : ID
     | namespace '.' ID ;

OPERATOR : '=='
     | '!='
     | '<'
     | '<='
     | '>'
     | '>='
     | '&&'
     | '||' ;

ID : [A-Za-z0-9_-]+ ;
WS : [ \t\r\n]+ -> skip ; // skip spaces, tabs, newlines