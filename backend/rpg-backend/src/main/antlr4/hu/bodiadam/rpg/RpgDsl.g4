grammar RpgDsl;

//Program felépítése
program
    : topLevelBlock* worldBlock EOF
    ;

// Globális blokkok
topLevelBlock
    : initialPromptBlock
    | itemsBlock
    | actorsBlock
    | variablesBlock
    | startBlock
    | rulesBlock
    ;

// Top-level blokkok
initialPromptBlock
    : INIT_PROMPT '{' STRING '}'
    ;

itemsBlock
    : ITEMS '{' (itemDefinition | instantiation)* '}'
    ;

itemDefinition
    : STACKABLE? ITEM ID (IS ID)? '{' itemBody '}'
    ;

itemBody
    : descriptionDefinition propertyDefinition*
    ;

actorsBlock
    : ACTORS '{' (actorDefinition | instantiation)* '}'
    ;

actorDefinition
    : ACTOR ID (IS ID)? '{' actorBody '}'
    ;

actorBody
    : descriptionDefinition propertyDefinition*
    ;

variablesBlock
    : VARS '{' variableDefinition* '}'
    ;

variableDefinition
    : ID ':' expression
    ;

startBlock
    : START '{' statement* '}'
    ;

rulesBlock
    : RULES '{' ruleDefinition* '}'
    ;

ruleDefinition
    : RULE ID ('(' parameters? ')')? (CHANCE INT)? '{' ruleBody '}'
    ;

parameters
    : ID (',' ID)*
    ;

ruleBody
    : requiresBlock? successBlock failBlock
    ;

requiresBlock
    : REQUIRES '{' condition '}'
    ;

successBlock
    : SUCCESS '{' statement* '}'
    ;

failBlock
    : FAIL '{' statement* '}'
    ;

// World és Scenes
worldBlock
    : WORLD '{' scenesBlock '}'
    ;

scenesBlock
    : SCENES '{' sceneDefinition* '}'
    ;

sceneDefinition
    : SCENE ID '{' sceneBody '}'
    ;

sceneBody
    : descriptionDefinition sceneElement*
    ;

// A jelenet lehetséges elemei.
sceneElement
    : presentDefinition
    | onEnter
    | onExit
    | intentsBlock
    | onIntent
    | endScene
    ;

descriptionDefinition
    : DESCRIPTION ':' STRING
    ;

propertyDefinition
    : ID ':' expression
    ;

presentDefinition
    : PRESENT '{' ID* '}'
    ;

onEnter
    : ON ENTER '{' statement* '}'
    ;

onExit
    : ON EXIT '{' statement* '}'
    ;

intentsBlock
    : INTENTS '{' intentDefinition* '}'
    ;

intentDefinition
    : INTENT ID '{' descriptionDefinition '}'
    ;

onIntent
    : ON ID CHANCE INT '{' intentBody '}'
    ;

intentBody
    : requiresBlock? successBlock failBlock
    ;

endScene
    : END SCENE
    ;

//Statements és conditions
condition
    : conditionTerm ((AND | OR) conditionTerm)*
    ;


conditionTerm
    : HAS ID
    | DOES NOT HAVE ID
    | propertyReference comparison expression
    | '(' condition ')'
    ;

propertyReference
    : ID ('.' ID)?
    ;

comparison
    : IS LESS THAN
    | IS GREATER THAN
    | IS EQUAL TO
    | IS NOT EQUAL TO
    | IS AT LEAST
    | IS AT MOST
    ;

expression
    : literal
    | propertyReference
    | '(' expression ')'
    ;

literal
    : INT
    | BOOLEAN
    | STRING
    ;

//Statements
statement
    : giveStatement
    | consumeStatement
    | addStatement
    | subtractStatement
    | multiplyStatement
    | divideStatement
    | narrateStatement
    | randomStatement
    | saysStatement
    | goToStatement
    | useStatement
    | instantiation
    ;

giveStatement
    : GIVE expression? ID
    ;

consumeStatement
    : CONSUME expression ID
    ;

addStatement
    : ADD expression TO propertyReference
    ;

subtractStatement
    : SUBTRACT expression FROM propertyReference
    ;

multiplyStatement
    : MULTIPLY propertyReference BY expression
    ;

divideStatement
    : DIVIDE propertyReference BY expression
    ;

narrateStatement
    : NARRATE STRING
    ;

randomStatement
    : RANDOM INT '{' statement* '}'
    ;

saysStatement
    : ID SAYS STRING
    ;

goToStatement
    : GO TO ID
    ;

useStatement
    : USE ID ('(' arguments? ')')?
    ;

arguments
    : expression (',' expression)*
    ;

instantiation
    : ID AS ID
    ;

// Lexer Szabályok
// Kulcsszavak
INIT_PROMPT : 'initPrompt' ;
ITEMS       : 'items' ;
STACKABLE   : 'stackable' ;
ITEM        : 'item' ;
IS          : 'is' ;
DESCRIPTION : 'description' ;
ACTORS      : 'actors' ;
ACTOR       : 'actor' ;
VARS        : 'vars' ;
START       : 'start' ;
RULES       : 'rules' ;
RULE        : 'rule' ;
CHANCE      : 'chance' ;
REQUIRES    : 'requires' ;
SUCCESS     : 'success' ;
FAIL        : 'fail' ;
WORLD       : 'world' ;
SCENES      : 'scenes' ;
SCENE       : 'scene' ;
PRESENT     : 'present' ;
ON          : 'on' ;
ENTER       : 'enter' ;
EXIT        : 'exit' ;
INTENTS     : 'intents' ;
INTENT      : 'intent' ;
END         : 'end' ;
GIVE        : 'give' ;
CONSUME     : 'consume' ;
ADD         : 'add' ;
TO          : 'to' ;
SUBTRACT    : 'subtract' ;
FROM        : 'from' ;
MULTIPLY    : 'multiply' ;
BY          : 'by' ;
DIVIDE      : 'divide' ;
NARRATE     : 'narrate' ;
RANDOM      : 'random' ;
SAYS        : 'says' ;
GO          : 'go' ;
USE         : 'use' ;
AS          : 'as' ;
AND         : 'and' ;
OR          : 'or' ;
HAS         : 'has' ;
DOES        : 'does' ;
NOT         : 'not' ;
HAVE        : 'have' ;
LESS        : 'less' ;
THAN        : 'than' ;
GREATER     : 'greater' ;
EQUAL       : 'equal' ;
AT          : 'at' ;
LEAST       : 'least' ;
MOST        : 'most' ;

// Literálok és Azonosítók
BOOLEAN : 'true' | 'false' ;
ID      : [a-zA-Z_][a-zA-Z0-9_]* ;
INT     : [0-9]+ ;
STRING  : '"' (~["\\] | '\\' .)* '"' ;

WS : [ \t\r\n]+ -> skip ;

BLOCK_COMMENT : '/*' .*? '*/' -> skip ;
LINE_COMMENT  : '//' ~[\r\n]* -> skip ;
