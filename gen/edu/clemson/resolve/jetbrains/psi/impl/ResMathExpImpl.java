// This is a generated file. Not intended for manual editing.
package edu.clemson.resolve.jetbrains.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static edu.clemson.resolve.jetbrains.ResTypes.*;
import edu.clemson.resolve.jetbrains.psi.*;
import com.intellij.psi.ResolveState;

public class ResMathExpImpl extends ResCompositeElementImpl implements ResMathExp {

  public ResMathExpImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof ResVisitor) ((ResVisitor)visitor).visitMathExp(this);
    else super.accept(visitor);
  }

  @Nullable
  public ResMathExp getResMathMetaTypeExp(ResolveState context) {
    return ResPsiImplUtil.getResMathMetaTypeExp(this, context);
  }

}