// This is a generated file. Not intended for manual editing.
package edu.clemson.resolve.jetbrains.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface ResUsesList extends ResCompositeElement {

  @NotNull
  List<ResUsesSpecGroup> getUsesSpecGroupList();

  @Nullable
  PsiElement getLparen();

  @Nullable
  PsiElement getRparen();

  @Nullable
  PsiElement getSemicolon();

  @NotNull
  PsiElement getUses();

}
