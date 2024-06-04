package wiki.minecraft.heywiki.wiki;

import com.mojang.logging.LogUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import wiki.minecraft.heywiki.HeyWikiConfig;
import wiki.minecraft.heywiki.gui.screen.HeyWikiConfirmLinkScreen;
import wiki.minecraft.heywiki.resource.WikiFamilyConfigManager;
import wiki.minecraft.heywiki.resource.WikiTranslationManager;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

public class WikiPage {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final MinecraftClient client = MinecraftClient.getInstance();
    public String pageName;
    public WikiIndividual wiki;

    public WikiPage(String pageName, WikiIndividual wiki) {
        this.pageName = pageName;
        this.wiki = wiki;
    }

    private static @Nullable String getOverride(WikiIndividual wiki, String translationKey) {
        return wiki.language().langOverride().map(s -> {
            if (WikiTranslationManager.translations.get(s).hasTranslation(translationKey)) {
                return WikiTranslationManager.translations.get(s).get(translationKey);
            } else {
                return null;
            }
        }).orElse(null);
    }

    public static @Nullable WikiPage fromTarget(Target target) {
        return fromTarget(target.identifier, target.translationKey);
    }

    public static @Nullable WikiPage fromTarget(Identifier identifier, String translationKey) {
        var family = WikiFamilyConfigManager.getFamilyByNamespace(identifier.getNamespace());
        if (family == null) return null;

        if (HeyWikiConfig.language.equals("auto")) {
            var language = client.options.language;
            var wiki = family.getLanguageWikiByGameLanguage(language);
            if (wiki != null) {
                String override = getOverride(wiki, translationKey);
                if (override != null) {
                    return new WikiPage(override, wiki);
                }
                return new WikiPage(I18n.translate(translationKey), wiki);
            }
        } else {
            var language = HeyWikiConfig.language;
            var wiki = family.getLanguageWikiByWikiLanguage(language);
            if (wiki != null) {
                if (wiki.language().matchLanguage(client.options.language)) {
                    String override = getOverride(wiki, translationKey);
                    if (override != null) {
                        return new WikiPage(override, wiki);
                    }
                    return new WikiPage(I18n.translate(translationKey), wiki);
                } else {
                    String override = getOverride(wiki, translationKey);
                    if (override != null) {
                        return new WikiPage(override, wiki);
                    }
                    return new WikiPage(WikiTranslationManager.translations
                            .get(wiki.language().defaultLanguage())
                            .get(translationKey), wiki);
                }
            }
        }

        WikiIndividual wiki = Objects.requireNonNull(family.getMainLanguageWiki());
        return new WikiPage(WikiTranslationManager.translations
                .get(wiki.language().defaultLanguage())
                .get(translationKey), wiki);
    }

    public static WikiPage fromWikitextLink(String link) {
        String[] split = link.split(":", 3);
        if (split.length == 1) {
            var family = WikiFamilyConfigManager.getFamilyByNamespace("minecraft");
            // [[Grass]]
            return new WikiPage(link, getWiki(family));
        }

        WikiIndividual languageWiki = Objects.requireNonNull(WikiFamilyConfigManager.getFamilyByNamespace("minecraft"))
                                             .getLanguageWikiByWikiLanguage(split[0]);
        if (languageWiki != null) {
            // valid language: [[en:Grass]]
            return new WikiPage(link.split(":", 2)[1], languageWiki);
        }

        if (WikiFamilyConfigManager.getAvailableNamespaces().contains(split[0])) {
            // valid NS
            if (split.length == 3) {
                WikiFamily family = Objects.requireNonNull(WikiFamilyConfigManager.getFamilyByNamespace(split[0]));
                WikiIndividual languageWiki1 = family.getLanguageWikiByWikiLanguage(split[1]);
                if (languageWiki1 != null) {
                    // valid language: [[minecraft:en:Grass]]
                    return new WikiPage(split[2], languageWiki1);
                }
            }
            // invalid language: [[minecraft:Grass]]
            return new WikiPage(link.split(":", 2)[1], getWiki(WikiFamilyConfigManager.getFamilyByNamespace(split[0])));
        }

        // [[Minecraft Legend:Grass]]
        return new WikiPage(link, getWiki(WikiFamilyConfigManager.getFamilyByNamespace("minecraft")));
    }

    public static WikiIndividual getWiki(WikiFamily family) {
        WikiIndividual wiki;

        if (HeyWikiConfig.language.equals("auto")) {
            var language = client.options.language;
            wiki = family.getLanguageWikiByGameLanguage(language);
        } else {
            var language = HeyWikiConfig.language;
            wiki = family.getLanguageWikiByWikiLanguage(language);
        }

        if (wiki == null) wiki = family.getLanguageWikiByGameLanguage("en_us");

        if (wiki == null) {
            LOGGER.error("Failed to find wiki for language {}", HeyWikiConfig.language);
            return null;
        }

        return wiki;
    }

    public static @Nullable WikiPage random(WikiFamily family) {
        WikiIndividual wiki = Objects.requireNonNull(getWiki(family));
        if (wiki.randomArticle().isEmpty()) return null;
        return new WikiPage(wiki.randomArticle().get(), wiki);
    }

    public static @Nullable WikiPage versionArticle(String version) {
        var family = WikiFamilyConfigManager.getFamilyByNamespace("minecraft");
        WikiIndividual wiki = Objects.requireNonNull(getWiki(family));
        Optional<String> name = wiki.versionArticle();
        return name.map(s -> new WikiPage(s.formatted(version), wiki)).orElse(null);
    }

    public @Nullable URI getUri() {
        try {
            return new URI(this.wiki.articleUrl().formatted(URLEncoder.encode(this.pageName.replaceAll(" ", "_"), StandardCharsets.UTF_8)));
        } catch (URISyntaxException e) {
            LOGGER.error("Failed to create URI for wiki page", e);
            return null;
        }
    }

    public void openInBrowser() {
        openInBrowser(false);
    }

    public void openInBrowser(Boolean skipConfirmation) {
        openInBrowser(skipConfirmation, null);
    }

    public void openInBrowser(Boolean skipConfirmation, Screen parent) {
        var uri = getUri();
        if (uri != null) {
            if (HeyWikiConfig.requiresConfirmation && !skipConfirmation) {
                HeyWikiConfirmLinkScreen.open(parent, uri.toString(), PageExcerpt.fromPage(this), this);
            } else {
                Util.getOperatingSystem().open(uri);
            }
        }
    }
}
