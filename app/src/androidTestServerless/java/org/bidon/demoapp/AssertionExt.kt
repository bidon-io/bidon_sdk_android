package org.bidon.demoapp

import android.view.View
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matcher

/**
 * Created by Bidon Team on 08/03/2023.
 */

fun ComposeContentTestRule.checkTextOnScreen(
    text: String,
    timeoutMillis: Long = defaultTimeout
) {
    waitUntilExists(hasText(text, substring = true), timeoutMillis)
}

fun ComposeContentTestRule.waitUntilNodeCount(
    matcher: SemanticsMatcher,
    count: Int,
    timeoutMillis: Long = defaultTimeout
) {
    this.waitUntil(timeoutMillis) {
        this.onAllNodes(matcher).fetchSemanticsNodes().size == count
    }
}

fun ComposeContentTestRule.waitUntilExists(
    matcher: SemanticsMatcher,
    timeoutMillis: Long = defaultTimeout
) {
    return this.waitUntilNodeCount(matcher, 1, timeoutMillis)
}

fun ComposeContentTestRule.waitUntilDoesNotExist(
    matcher: SemanticsMatcher,
    timeoutMillis: Long = defaultTimeout
) {
    return this.waitUntilNodeCount(matcher, 0, timeoutMillis)
}

fun clickOnXmlButton(buttonDescription: String) {
    onView(ViewMatchers.withContentDescription(buttonDescription)).perform(
        object : ViewAction {
            override fun getDescription(): String {
                return buttonDescription
            }

            override fun getConstraints(): Matcher<View> {
                return ViewMatchers.isEnabled()
            }

            override fun perform(uiController: UiController?, view: View?) {
                view?.performClick()
            }
        }
    )
}

fun clickBackButton() {
//    onView(ViewMatchers.isRoot()).perform(ViewActions.closeSoftKeyboard())
//    onView(ViewMatchers.isRoot()).perform(ViewActions.pressBack())
    pressBack()
}

fun ComposeContentTestRule.clickOnComposeButton(text: String) = onNodeWithText(text).performClick()

private const val defaultTimeout = 15_000L