package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil

class NoConsecutiveBlankLinesRule : Rule("no-consecutive-blank-lines") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node is PsiWhiteSpace) {
            val text = node.getText()
            val lfcount = text.count { it == '\n' }
            if (lfcount < 2) {
                return
            }
            val eof = PsiTreeUtil.nextLeaf(node) == null
            if (lfcount > 2 || eof) {
                val split = text.split("\n")
                emit(node.startOffset + split[0].length + split[1].length + 2, "Needless blank line(s)", true)
                if (autoCorrect) {
                    (node as LeafPsiElement)
                        .rawReplaceWithText("${split.first()}\n${if (eof) "" else "\n"}${split.last()}")
                }
            }
        }
    }
}
