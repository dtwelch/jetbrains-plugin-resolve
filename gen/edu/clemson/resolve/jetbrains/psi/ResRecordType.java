// This is a generated file. Not intended for manual editing.
package edu.clemson.resolve.jetbrains.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface ResRecordType extends ResType {

  @NotNull
  List<ResRecordVarDeclGroup> getRecordVarDeclGroupList();

  @NotNull
  PsiElement getRecord();

  @Nullable
  PsiElement getEnd();

}
