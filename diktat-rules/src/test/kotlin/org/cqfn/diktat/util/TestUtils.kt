package org.cqfn.diktat.util

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.RuleSet
import org.assertj.core.api.Assertions
import org.assertj.core.api.SoftAssertions
import org.cqfn.diktat.common.config.rules.RulesConfig
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.cqfn.diktat.ruleset.utils.log
import java.util.concurrent.atomic.AtomicInteger

const val TEST_FILE_NAME = "/TestFileName.kt"

@Suppress("ForbiddenComment")
fun lintMethod(rule: Rule,
               code: String,
               vararg lintErrors: LintError,
               rulesConfigList: List<RulesConfig>? = null,
               fileName: String? = null) {
    val res = mutableListOf<LintError>()
    KtLint.lint(
            KtLint.Params(
                    fileName = fileName ?: TEST_FILE_NAME,
                    text = code,
                    ruleSets = listOf(DiktatRuleSetProvider4Test(rule, rulesConfigList).get()),
                    cb = { e, _ -> res.add(e) }
            )
    )
    Assertions.assertThat(res)
            .hasSize(lintErrors.size)
            .allSatisfy { actual ->
                val expected = lintErrors[res.indexOf(actual)]
                SoftAssertions.assertSoftly {
                    it.assertThat(actual.line).`as`("Line").isEqualTo(expected.line)
                    it.assertThat(actual.col).`as`("Column").isEqualTo(expected.col)
                    it.assertThat(actual.ruleId).`as`("Rule id").isEqualTo(expected.ruleId)
                    it.assertThat(actual.detail).`as`("Detailed message").isEqualTo(expected.detail)
                    it.assertThat(actual.canBeAutoCorrected).`as`("Can be autocorrected").isEqualTo(expected.canBeAutoCorrected)
                }
            }
}

internal fun Rule.format(text: String, fileName: String,
                         rulesConfigList: List<RulesConfig>? = emptyList()): String {
    return KtLint.format(
            KtLint.Params(
                    text = text,
                    ruleSets = listOf(DiktatRuleSetProvider4Test(this, rulesConfigList).get()),
                    fileName = fileName,
                    cb = { lintError, _ ->
                        log.warn("Received linting error: $lintError")
                    }
            )
    )
}

/**
 * This utility function lets you run arbitrary code on every node of given [code].
 * It also provides you with counter which can be incremented inside [applyToNode] and then will be compared to [expectedAsserts].
 * This allows you to keep track of how many assertions have actually been run on your code during tests.
 */
internal fun applyToCode(code: String,
                         expectedAsserts: Int,
                         applyToNode: (node: ASTNode, counter: AtomicInteger) -> Unit) {
    val counter = AtomicInteger(0)
    KtLint.lint(
            KtLint.Params(
                    text = code,
                    ruleSets = listOf(
                            RuleSet("test", object : Rule("astnode-utils-test") {
                                override fun visit(node: ASTNode,
                                                   autoCorrect: Boolean,
                                                   params: KtLint.Params,
                                                   emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {
                                    applyToNode(node, counter)
                                }
                            })
                    ),
                    cb = { _, _ -> Unit }
            )
    )
    Assertions.assertThat(counter.get()).`as`("Number of expected asserts").isEqualTo(expectedAsserts)
}
