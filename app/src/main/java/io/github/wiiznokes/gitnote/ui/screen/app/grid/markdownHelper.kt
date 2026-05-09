package io.github.wiiznokes.gitnote.ui.screen.app.grid

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import com.mikepenz.markdown.compose.MarkdownSuccess
import com.mikepenz.markdown.compose.components.MarkdownComponents
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.m3.elements.MarkdownCheckBox
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.ImageTransformer
import com.mikepenz.markdown.model.MarkdownAnimations
import com.mikepenz.markdown.model.MarkdownAnnotator
import com.mikepenz.markdown.model.MarkdownColors
import com.mikepenz.markdown.model.MarkdownDimens
import com.mikepenz.markdown.model.MarkdownExtendedSpans
import com.mikepenz.markdown.model.MarkdownInlineContent
import com.mikepenz.markdown.model.MarkdownPadding
import com.mikepenz.markdown.model.MarkdownTypography
import com.mikepenz.markdown.model.NoOpImageTransformerImpl
import com.mikepenz.markdown.model.ReferenceLinkHandler
import com.mikepenz.markdown.model.ReferenceLinkHandlerImpl
import com.mikepenz.markdown.model.State
import com.mikepenz.markdown.model.markdownAnimations
import com.mikepenz.markdown.model.markdownAnnotator
import com.mikepenz.markdown.model.markdownDimens
import com.mikepenz.markdown.model.markdownExtendedSpans
import com.mikepenz.markdown.model.markdownInlineContent
import com.mikepenz.markdown.model.markdownPadding
import org.intellij.markdown.flavours.MarkdownFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser


@Composable
fun MarkdownCustom(
    content: String,
    onClick: () -> Unit,
) {
    CompositionLocalProvider(
        LocalUriHandler provides object : UriHandler {
            override fun openUri(uri: String) {
                onClick()
            }
        }
    ) {
        MarkdownCustomInner(
            content = content,
            modifier = Modifier
                .verticalScroll(state = rememberScrollState(), enabled = false)
        )
    }
}
@Composable
fun MarkdownCustomInner(
    content: String,
    colors: MarkdownColors = markdownColor(),
    typography: MarkdownTypography = markdownTypographyScaled(),
    modifier: Modifier = Modifier.fillMaxSize(),
    padding: MarkdownPadding = markdownPadding(),
    dimens: MarkdownDimens = markdownDimens(),
    flavour: MarkdownFlavourDescriptor = GFMFlavourDescriptor(),
    parser: MarkdownParser = MarkdownParser(flavour),
    imageTransformer: ImageTransformer = NoOpImageTransformerImpl(),
    annotator: MarkdownAnnotator = markdownAnnotator(),
    extendedSpans: MarkdownExtendedSpans = markdownExtendedSpans(),
    inlineContent: MarkdownInlineContent = markdownInlineContent(),
    components: MarkdownComponents = markdownComponents(checkbox = { MarkdownCheckBox(it.content, it.node, it.typography.text) }),
    animations: MarkdownAnimations = markdownAnimations(),
    referenceLinkHandler: ReferenceLinkHandler = ReferenceLinkHandlerImpl(),
    lookupLinks: Boolean = true,
    loading: @Composable (modifier: Modifier) -> Unit = { Box(modifier) },
    success: @Composable (state: State.Success, components: MarkdownComponents, modifier: Modifier) -> Unit = { state, components, modifier ->
        MarkdownSuccess(state = state, components = components, modifier = modifier)
    },
    error: @Composable (modifier: Modifier) -> Unit = { Box(modifier) },
) = com.mikepenz.markdown.compose.Markdown(
    content = content,
    colors = colors,
    typography = typography,
    modifier = modifier,
    padding = padding,
    dimens = dimens,
    flavour = flavour,
    parser = parser,
    imageTransformer = imageTransformer,
    annotator = annotator,
    extendedSpans = extendedSpans,
    inlineContent = inlineContent,
    components = components,
    animations = animations,
    referenceLinkHandler = referenceLinkHandler,
    lookupLinks = lookupLinks,
    loading = loading,
    success = success,
    error = error,
)



private fun TextStyle.scaled(scale: Float): TextStyle = copy(
    fontSize = fontSize.scale(scale),
    lineHeight = lineHeight.scale(scale),
    letterSpacing = letterSpacing.scale(scale)
)

private fun TextUnit.scale(scale: Float): TextUnit =
    if (this.isSpecified) this * scale else this

@Composable
fun markdownTypographyScaled(
    scale: Float = 0.85f
): MarkdownTypography = markdownTypography(
    h1 = MaterialTheme.typography.headlineLarge,
    h2 = MaterialTheme.typography.headlineMedium,
    h3 = MaterialTheme.typography.headlineSmall,
    h4 = MaterialTheme.typography.titleLarge,
    h5 = MaterialTheme.typography.titleMedium,
    h6 = MaterialTheme.typography.titleSmall,
    text = MaterialTheme.typography.bodyLarge.scaled(scale),
    code = MaterialTheme.typography.bodyMedium
        .scaled(scale)
        .copy(fontFamily = FontFamily.Monospace),
    inlineCode = MaterialTheme.typography.bodyLarge
        .scaled(scale)
        .copy(fontFamily = FontFamily.Monospace),
    quote = MaterialTheme.typography.bodyMedium
        .scaled(scale)
        .plus(SpanStyle(fontStyle = FontStyle.Italic)),
    paragraph = MaterialTheme.typography.bodyLarge.scaled(scale),
    ordered = MaterialTheme.typography.bodyLarge.scaled(scale),
    bullet = MaterialTheme.typography.bodyLarge.scaled(scale),
    list = MaterialTheme.typography.bodyLarge.scaled(scale),
    table = MaterialTheme.typography.bodyLarge.scaled(scale),
    textLink = TextLinkStyles(
        style = MaterialTheme.typography.bodyLarge
            .scaled(scale)
            .copy(
                fontWeight = FontWeight.Bold,
                textDecoration = TextDecoration.Underline
            )
            .toSpanStyle()
    )
)