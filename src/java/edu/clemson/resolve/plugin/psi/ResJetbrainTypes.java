package edu.clemson.resolve.plugin.psi;

import edu.clemson.resolve.plugin.RESOLVETokenTypes;
import edu.clemson.resolve.plugin.parser.Resolve;
import edu.clemson.resolve.plugin.parser.ResolveLexer;
import org.antlr.intellij.adaptor.lexer.RuleElementType;
import org.antlr.intellij.adaptor.lexer.TokenElementType;

import static edu.clemson.resolve.plugin.RESOLVETokenTypes.RULE_ELEMENT_TYPES;
import static edu.clemson.resolve.plugin.RESOLVETokenTypes.TOKEN_ELEMENT_TYPES;

/**
 * This interface emulates the <tt>LANGTypes</tt> file that is autogenerated
 * by the jetbrains grammarkit tool.
 *
 * <p>Having these tokens explicitly stored here, rather than packed into
 * {@link com.intellij.psi.tree.TokenSet}s (as they are in {@link RESOLVETokenTypes})
 * makes doing things like writing keyword completion patterns cleaner.</p>
 *
 * @since 0.0.1
 */
public interface ResJetbrainTypes {

    static RuleElementType MATH_SYMBOL_NAME = RULE_ELEMENT_TYPES.get(Resolve.RULE_mathSymbolName);

    static TokenElementType ID = TOKEN_ELEMENT_TYPES.get(ResolveLexer.ID);
}
