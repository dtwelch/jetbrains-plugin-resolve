package edu.clemson.resolve.plugin.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import edu.clemson.resolve.plugin.ResTypes;
import java.util.*;
import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static edu.clemson.resolve.plugin.RESOLVEParserDefinition.*;

%%

%{
  public _ResLexer() {
    this((java.io.Reader)null);
  }
%}

%unicode
%class _ResLexer
%implements FlexLexer, ResTypes
%function advance
%type IElementType

%eof{
  return;
%eof}

NL = [\r\n] | \r\n      // NewLine
WS = [ \t\f]            // Whitespaces

LINE_COMMENT = "//" [^\r\n]*

LETTER = [:letter:] | "_"
DIGIT =  [:digit:]

INT_DIGIT = [0-9]
NUM_INT = "0" | ([1-9] {INT_DIGIT}*)

IDENT = {LETTER} ({LETTER} | {DIGIT} )*
STR =      "\""
ESCAPES = [abfnrtv]

%%
<YYINITIAL> {

{WS}                                    { return WS; }
{NL}+                                   { return NLS; }

"/*" ( ([^"*"]|[\r\n])* ("*"+ [^"*""/"] )? )* ("*" | "*"+"/")? { return MULTILINE_COMMENT; }
{STR} ( [^\"\\\n\r] | "\\" ("\\" | {STR} | {ESCAPES} | [0-8xuU] ) )* {STR}? { return STRING; }
"`" [^`]* "`"?                          { return RAW_STRING; }

{LINE_COMMENT}                          { return LINE_COMMENT; }
"("                                     { return LPAREN; }
")"                                     { return RPAREN; }
"."                                     { return DOT; }
":"                                     { return COLON; }
";"                                     { return SEMICOLON; }
","                                     { return COMMA; }
"Facility"                              { return FACILITY;  }
"Concept"                               { return CONCEPT;  }
"end"                                   { return END;  }
"uses"                                  { return USES; }
"is"                                    { return IS; }
"externally"                            { return EXTERNALLY; }
"implemented"                           { return IMPLEMENTED; }
"by"                                    { return BY; }
"Type"                                  { return TYPE; }
"Family"                                { return FAMILY; }
"modeled"                               { return MODELED; }
"exemplar"                              { return EXEMPLAR; }
"Operation"                             { return OPERATION; }
"Recursive"                             { return RECURSIVE; }
"Procedure"                             { return PROCEDURE; }
{IDENT}                                 { return IDENTIFIER; }
{NUM_INT}                               { return INT; }
.                                       { return BAD_CHARACTER; }
}