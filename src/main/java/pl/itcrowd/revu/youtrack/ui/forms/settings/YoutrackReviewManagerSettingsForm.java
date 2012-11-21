package pl.itcrowd.revu.youtrack.ui.forms.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.jetbrains.annotations.Nls;
import org.sylfra.idea.plugins.revu.RevuIconProvider;
import pl.itcrowd.revu.youtrack.business.YoutrackReviewManager;
import pl.itcrowd.revu.youtrack.settings.project.RevuOnYoutrackSettings;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class YoutrackReviewManagerSettingsForm implements Configurable {
// ------------------------------ FIELDS ------------------------------

    private JTextField endLine;

    private JTextField file;

    private JPasswordField password;

    private JTextField priority;

    private JTextField priorityBundleName;

    private JTextField projectID;

    private JPanel rootComponent;

    private JTextField startLine;

    private JTextField username;

    private JTextField vcsRev;

    private YoutrackReviewManager youtrackReviewManager;

    private JTextField youtrackURL;

// --------------------------- CONSTRUCTORS ---------------------------

    public YoutrackReviewManagerSettingsForm(Project project)
    {
        youtrackReviewManager = project.getComponent(YoutrackReviewManager.class);
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface Configurable ---------------------

    @Nls
    public String getDisplayName()
    {
        return "reVu on YouTrack";
    }

    public Icon getIcon()
    {
        return RevuIconProvider.getIcon(RevuIconProvider.IconRef.REVU_LARGE);
    }

    public String getHelpTopic()
    {
        return null;
    }

// --------------------- Interface UnnamedConfigurable ---------------------

    public JComponent createComponent()
    {
        return rootComponent;
    }

    public boolean isModified()
    {
        RevuOnYoutrackSettings state = youtrackReviewManager.getState();
        return !new EqualsBuilder().append(state.youtrackURL, youtrackURL.getText())
            .append(state.username, username.getText())
            .append(state.password, new String(password.getPassword()))
            .append(state.projectID, projectID.getText())
            .append(state.fileField, file.getText())
            .append(state.startLineField, startLine.getText())
            .append(state.endLineField, endLine.getText())
            .append(state.priorityField, priority.getText())
            .append(state.vcsRevField, vcsRev.getText())
            .append(state.priorityBundleName, priorityBundleName.getText())
            .isEquals();
    }

    public void apply() throws ConfigurationException
    {
        final RevuOnYoutrackSettings state = new RevuOnYoutrackSettings();
        state.youtrackURL = youtrackURL.getText();
        state.username = username.getText();
        state.password = new String(password.getPassword());
        state.projectID = projectID.getText();
        state.fileField = file.getText();
        state.startLineField = startLine.getText();
        state.endLineField = endLine.getText();
        state.priorityField = priority.getText();
        state.vcsRevField = vcsRev.getText();
        state.priorityBundleName = priorityBundleName.getText();
        youtrackReviewManager.loadState(state);
    }

    public void reset()
    {
        RevuOnYoutrackSettings state = youtrackReviewManager.getState();
        youtrackURL.setText(state.youtrackURL);
        username.setText(state.username);
        password.setText(state.password);
        projectID.setText(state.projectID);
        file.setText(state.fileField);
        startLine.setText(state.startLineField);
        endLine.setText(state.endLineField);
        priority.setText(state.priorityField);
        vcsRev.setText(state.vcsRevField);
        priorityBundleName.setText(state.priorityBundleName);
    }

    public void disposeUIResources()
    {
    }
}
