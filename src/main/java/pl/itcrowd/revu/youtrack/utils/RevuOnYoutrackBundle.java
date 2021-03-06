package pl.itcrowd.revu.youtrack.utils;

import com.intellij.CommonBundle;
import com.intellij.reference.SoftReference;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import javax.swing.JLabel;
import java.lang.ref.Reference;
import java.util.ResourceBundle;

/**
 * Provides convenient methods to retrieve labels from bundles declared in plugin conf
 *
 * @author <a href="mailto:syllant@gmail.com">Sylvain FRANCOIS</a>
 * @version $Id$
 */
public class RevuOnYoutrackBundle {
// ------------------------------ FIELDS ------------------------------

    @NonNls
    public static final String EMPTY_FIELD = "";

    @NonNls
    private static final String BUNDLE = "pl.itcrowd.revu.youtrack.resources.Bundle";

    private static Reference<ResourceBundle> pluginBundle;

// -------------------------- STATIC METHODS --------------------------

    public static void assignWithMnemonic(@NotNull String text, @NotNull JLabel label)
    {
        int mnemonicIndex = text.indexOf('\u001b');
        if (mnemonicIndex > -1) {
            text = text.substring(0, mnemonicIndex) + text.substring(mnemonicIndex + 1);
        }

        label.setText(text);
        if (mnemonicIndex != -1) {
            label.setDisplayedMnemonic(text.charAt(mnemonicIndex));
        } else {
            label.setDisplayedMnemonic(0);
        }
        label.setDisplayedMnemonicIndex(mnemonicIndex);
    }

    private static ResourceBundle getBundle()
    {
        ResourceBundle bundle = null;
        if (pluginBundle != null) {
            bundle = pluginBundle.get();
        }
        if (bundle == null) {
            bundle = ResourceBundle.getBundle(BUNDLE);
            pluginBundle = new SoftReference<ResourceBundle>(bundle);
        }
        return bundle;
    }

    /**
     * Returns the message got the specified key
     *
     * @param key the message key
     *
     * @return the formatted message
     */
    public static String message(@PropertyKey(resourceBundle = BUNDLE) String key)
    {
        return message(key, new Object[]{});
    }

    /**
     * Returns the message got the specified key, formatted with specified params
     *
     * @param key    the message key
     * @param params the message parameters
     *
     * @return the formatted message
     */
    public static String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params)
    {
        return CommonBundle.message(getBundle(), key, params);
    }
}
