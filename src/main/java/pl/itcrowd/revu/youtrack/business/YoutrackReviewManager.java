package pl.itcrowd.revu.youtrack.business;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.sylfra.idea.plugins.revu.business.IIssueListener;
import org.sylfra.idea.plugins.revu.business.ReviewManager;
import org.sylfra.idea.plugins.revu.model.History;
import org.sylfra.idea.plugins.revu.model.Issue;
import org.sylfra.idea.plugins.revu.model.IssueNote;
import org.sylfra.idea.plugins.revu.model.IssuePriority;
import org.sylfra.idea.plugins.revu.model.IssueStatus;
import org.sylfra.idea.plugins.revu.model.Review;
import org.sylfra.idea.plugins.revu.model.ReviewStatus;
import org.sylfra.idea.plugins.revu.model.User;
import org.sylfra.idea.plugins.revu.ui.statusbar.StatusBarComponent;
import org.sylfra.idea.plugins.revu.ui.statusbar.StatusBarMessage;
import org.sylfra.idea.plugins.revu.utils.RevuUtils;
import pl.com.it_crowd.youtrack.api.Command;
import pl.com.it_crowd.youtrack.api.Filter;
import pl.com.it_crowd.youtrack.api.IssueWrapper;
import pl.com.it_crowd.youtrack.api.YoutrackAPI;
import pl.com.it_crowd.youtrack.api.defaults.Fields;
import pl.com.it_crowd.youtrack.api.defaults.StateValues;
import pl.com.it_crowd.youtrack.api.rest.Comment;
import pl.com.it_crowd.youtrack.api.rest.Enumeration;
import pl.com.it_crowd.youtrack.api.rest.EnumerationValue;
import pl.itcrowd.revu.youtrack.model.ReVuYoutrackIssue;
import pl.itcrowd.revu.youtrack.settings.project.RevuOnYoutrackSettings;
import pl.itcrowd.revu.youtrack.utils.FileLocator;
import pl.itcrowd.revu.youtrack.utils.ReVuStringUtils;
import pl.itcrowd.revu.youtrack.utils.RevuOnYoutrackBundle;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

@State(
    name = YoutrackReviewManager.COMPONENT_NAME,
    storages = {@Storage(
        file = "$PROJECT_FILE$")})
public class YoutrackReviewManager implements ProjectComponent, PersistentStateComponent<RevuOnYoutrackSettings> {
// ------------------------------ FIELDS ------------------------------

    public static final String ADD_ISSUE_ERROR = "friendlyError.youtrackReview.addIssue.error.title.text";

    public static final String COMPONENT_NAME = "YouTrackReviewManager";

    public static final String DELETE_ISSUE_ERROR = "friendlyError.youtrackReview.deleteReview.error.title.text";

    public static final String REFRESH_REVIEW_ERROR = "friendlyError.youtrackReview.refreshReview.error.title.text";

    public static final String UPDATE_ISSUE_ERROR = "friendlyError.youtrackReview.updateReview.error.title.text";

    private final static Logger LOGGER = Logger.getInstance(YoutrackReviewManager.class.getName());

    private boolean loadingIssuesFromYoutrack = false;

    private Project project;

    private Review review;

    private RevuOnYoutrackSettings state;

    private YoutrackAPI youtrackAPI;

// -------------------------- STATIC METHODS --------------------------

    public static void handleException(String messageKey, Exception ex, Project project)
    {
        String errorTitle = RevuOnYoutrackBundle.message(messageKey);
        LOGGER.error(errorTitle, ex);
        StatusBarComponent.showMessageInPopup(project, (new StatusBarMessage(StatusBarMessage.Type.ERROR, errorTitle, ex.getMessage())), false);
    }

// --------------------------- CONSTRUCTORS ---------------------------

    public YoutrackReviewManager(Project project)
    {
        this.project = project;
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    @NotNull
    public RevuOnYoutrackSettings getState()
    {
        if (state == null) {
            state = new RevuOnYoutrackSettings();
        }
        return state;
    }

    private YoutrackAPI getYoutrackAPI() throws JAXBException, IOException
    {
        if (youtrackAPI == null) {
            youtrackAPI = new YoutrackAPI(getYoutrackServiceURL(), getYoutrackServiceUsername(), getYoutrackServicePassword());
        }
        return youtrackAPI;
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface BaseComponent ---------------------

    public void initComponent()
    {
    }

    public void disposeComponent()
    {
    }

// --------------------- Interface NamedComponent ---------------------

    @NotNull
    public String getComponentName()
    {
        return COMPONENT_NAME;
    }

// --------------------- Interface PersistentStateComponent ---------------------

    public void loadState(RevuOnYoutrackSettings state)
    {
        this.state = state;
    }

// --------------------- Interface ProjectComponent ---------------------

    public void projectOpened()
    {

    }

    public void projectClosed()
    {

    }

// -------------------------- OTHER METHODS --------------------------

    public void refreshReview() throws JAXBException, IOException
    {
        final ReviewManager reviewManager = project.getComponent(ReviewManager.class);
        if (reviewManager == null) {
            LOGGER.error(String.format("Missing component %s", ReviewManager.class.getCanonicalName()));
            return;
        }
        if (review != null) {
            reviewManager.removeReview(review);
        }
        review = new Review("[youtrack]");
        review.setEmbedded(false);
        review.getDataReferential().setIssuePriorities(toIssuePriorities(getYoutrackAPI().getBundle(getYoutrackPriorityBundleName())));
        loadIssuesFromYoutrack(review);
        review.addIssueListener(new YoutrackReviewIssueListener());
        review.setShared(true);
        review.setStatus(ReviewStatus.REVIEWING);
        review.setExternalizable(false);
        User currentUser = RevuUtils.getCurrentUser();
        if (currentUser != null) {
            final User userByLogin = review.getDataReferential().getUsersByLogin(true).get(currentUser.getLogin());
            if (userByLogin != null) {
                userByLogin.setDisplayName(currentUser.getDisplayName());
                userByLogin.setPassword(currentUser.getPassword());
                currentUser = userByLogin;
            }
            currentUser.addRole(User.Role.ADMIN);
            currentUser.addRole(User.Role.REVIEWER);
            currentUser.addRole(User.Role.AUTHOR);
            review.getDataReferential().addUser(currentUser);
        }
        reviewManager.addReview(review);
    }

    private String getRelativePath(VirtualFile file)
    {
        VirtualFile baseDir = project.getBaseDir();
        return baseDir == null ? file.getCanonicalPath() : VfsUtil.getRelativePath(file, baseDir, '/');
    }

    private User getUser(String youtrackUsername)
    {
        User user = review.getDataReferential().getUsersByLogin(true).get(youtrackUsername);
        if (user == null) {
            user = new User(youtrackUsername, null, youtrackUsername);
            review.getDataReferential().addUser(user);
        }
        return user;
    }

    private String getYoutrackFileField()
    {
        return getState().fileField;
    }

    private String getYoutrackLineEndField()
    {
        return getState().endLineField;
    }

    private String getYoutrackLineStartField()
    {
        return getState().startLineField;
    }

    private String getYoutrackPriorityBundleName()
    {
        return getState().priorityBundleName;
    }

    private String getYoutrackPriorityField()
    {
        return getState().priorityField;
    }

    private String getYoutrackProjectId()
    {
        return getState().projectID;
    }

    private String getYoutrackServicePassword()
    {
        return getState().password;
    }

    private String getYoutrackServiceURL()
    {
        return getState().youtrackURL;
    }

    private String getYoutrackServiceUsername()
    {
        return getState().username;
    }

    private String getYoutrackVcsRevField()
    {
        return getState().vcsRevField;
    }

    private void loadIssuesFromYoutrack(Review review) throws JAXBException, IOException
    {
        loadingIssuesFromYoutrack = true;
        try {
            int firstResult = 0;
            List<IssueWrapper> tickets;
            do {
                tickets = getYoutrackAPI().searchIssuesByProject(getYoutrackProjectId(),
                    Filter.stateFilter(StateValues.NotVerified, StateValues.NotObsolete).after(firstResult).maxResults(999999));
                for (IssueWrapper ticket : tickets) {
                    final Issue issue = toYoutrackIssue(ticket);
                    issue.setReview(review);
                    review.addIssue(issue);
                }
                firstResult += tickets.size();
            } while (!tickets.isEmpty());
        } finally {
            loadingIssuesFromYoutrack = false;
        }
    }

    private String makeDescription(Issue issue) throws IOException
    {
        final StringBuilder stringBuilder = new StringBuilder(issue.getDesc());
        final VirtualFile file = issue.getFile();
        if (file != null && !file.isDirectory()) {
            stringBuilder.append("\n\nFile:\n").append(getRelativePath(file));
            String lang = file.getFileType().getName();
            final String code = ReVuStringUtils.extractLines(new String(file.contentsToByteArray()), issue.getLineStart(), issue.getLineEnd());
            stringBuilder.append("\n\n{code:lang=").append(lang).append("}").append(code).append("{code}");
        }
        return stringBuilder.toString();
    }

    private Date toDate(String youtrackDate)
    {
        return new Date(Long.parseLong(youtrackDate));
    }

    private List<IssuePriority> toIssuePriorities(Enumeration bundle)
    {
        final List<IssuePriority> priorities = new ArrayList<IssuePriority>();
        List<EnumerationValue> values = bundle.getValues();
        for (int i = 0, valuesSize = values.size(); i < valuesSize; i++) {
            priorities.add(new IssuePriority((byte) i, values.get(i).getValue()));
        }
        return priorities;
    }

    private List<IssueNote> toNotes(List<Comment> comments)
    {
        final List<IssueNote> notes = new ArrayList<IssueNote>();
        for (Comment comment : comments) {
            final IssueNote note = new IssueNote();
            note.setContent(comment.getText());
            final History history = new History();
            history.setCreatedOn(new Date(comment.getCreated()));
            history.setCreatedBy(getUser(comment.getAuthor()));
            note.setHistory(history);
            notes.add(note);
        }
        return notes;
    }

    private IssuePriority toReVuPriority(@NotNull String youtrackPriority)
    {
        return review.getDataReferential().getIssuePriority(youtrackPriority);
    }

    private ReVuYoutrackIssue toYoutrackIssue(IssueWrapper ticket)
    {
        final ReVuYoutrackIssue issue = new ReVuYoutrackIssue(ticket.getId());
        issue.setSummary(ticket.getFieldValue(Fields.summary));
        issue.setDesc(ticket.getFieldValue(Fields.description));
        issue.setVcsRev(ticket.getFieldValue(getYoutrackVcsRevField()));
        final String youtrackPriority = ticket.getFieldValue(getYoutrackPriorityField());
        if (youtrackPriority != null) {
            issue.setPriority(toReVuPriority(youtrackPriority));
        }
        issue.getHistory().setCreatedOn(toDate(ticket.getFieldValue(Fields.created)));
        issue.getHistory().setCreatedBy(getUser(ticket.getFieldValue(Fields.reporterName)));
        issue.getHistory().setLastUpdatedOn(toDate(ticket.getFieldValue(Fields.updated)));
        issue.getHistory().setLastUpdatedBy(getUser(ticket.getFieldValue(Fields.updaterName)));
        final String ticketState = ticket.getFieldValue(Fields.state);
        if (StateValues.Reopened.toString().equals(ticketState)) {
            issue.setStatus(IssueStatus.REOPENED);
        } else if (StateValues.InProgress.toString().equals(ticketState) || StateValues.New.toString().equals(ticketState) || StateValues.Open
            .toString()
            .equals(ticketState) || StateValues.Submitted.toString().equals(ticketState)) {
            issue.setStatus(IssueStatus.TO_RESOLVE);
        } else if (StateValues.Verified.toString().equals(ticketState)) {
            issue.setStatus(IssueStatus.CLOSED);
        } else {
            issue.setStatus(IssueStatus.RESOLVED);
        }
        final String assignee = ticket.getFieldValue(Fields.assignee);
        if (!StringUtils.isBlank(assignee)) {
            issue.getAssignees().add(getUser(assignee));
        }
        final String ticketFieldValue = ticket.getFieldValue(getYoutrackFileField());
        if (!StringUtils.isBlank(ticketFieldValue)) {
            final VirtualFile issueFile = FileLocator.findRelativeFile(ticketFieldValue, project.getBaseDir());
            issue.setFile(issueFile);
        }

        final String value = ticket.getFieldValue(getYoutrackLineEndField());
        if (!StringUtils.isBlank(value)) {
            try {
                issue.setLineEnd(Integer.parseInt(value));
            } catch (NumberFormatException ex) {
                LOGGER.error(String.format("Cannot set line end for ticket %s", ticket.getId()), ex);
            }
        }

        final String lineEndValue = ticket.getFieldValue(getYoutrackLineStartField());
        if (!StringUtils.isBlank(value)) {
            try {
                issue.setLineStart(Integer.parseInt(lineEndValue));
            } catch (NumberFormatException ex) {
                LOGGER.error(String.format("Cannot set line start for ticket %s", ticket.getId()), ex);
            }
        }

        issue.setNotes(toNotes(ticket.getComments()));
        return issue;
    }

// -------------------------- INNER CLASSES --------------------------

    private class YoutrackReviewIssueListener implements IIssueListener {
// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface IIssueListener ---------------------

        public void issueAdded(Issue issue)
        {
            if (loadingIssuesFromYoutrack) {
                return;
            }
            try {
                doIssueAdded(issue);
            } catch (Exception ex) {
                handleException(ADD_ISSUE_ERROR, ex, project);
            }
        }

        public void issueDeleted(Issue issue)
        {
            if (issue instanceof ReVuYoutrackIssue) {
                try {
                    getYoutrackAPI().deleteIssue(((ReVuYoutrackIssue) issue).getTicket());
                } catch (Exception ex) {
                    handleException(DELETE_ISSUE_ERROR, ex, project);
                }
            }
        }

        public void issueUpdated(Issue issue)
        {
            if (!(issue instanceof ReVuYoutrackIssue)) {
                return;
            }
            try {
                updateIssue((ReVuYoutrackIssue) issue, false);
            } catch (Exception ex) {
                handleException(UPDATE_ISSUE_ERROR, ex, project);
            }
        }

        private void doIssueAdded(Issue issue) throws IOException, JAXBException
        {
            if (issue instanceof ReVuYoutrackIssue) {
                return;
            }
            final String description = makeDescription(issue);
            issue.setDesc(description);
            final String ticketId = getYoutrackAPI().createIssue(getYoutrackProjectId(), issue.getSummary(), issue.getDesc());
            final ReVuYoutrackIssue youtrackIssue = toYoutrackIssue(getYoutrackAPI().getIssue(ticketId));
            youtrackIssue.copyFrom(issue);
            updateIssue(youtrackIssue, true);
            issue.getReview().removeIssue(issue);
            issue.getReview().addIssue(youtrackIssue);
        }

        private void updateIssue(ReVuYoutrackIssue issue, boolean disableNotifications) throws IOException, JAXBException
        {
            getYoutrackAPI().updateIssue(issue.getTicket(), issue.getSummary(), issue.getDesc());
            if (issue.getAssignees().size() > 0) {
                final Iterator<User> iterator = issue.getAssignees().iterator();
                final String login = iterator.next().getLogin();
                while (iterator.hasNext()) {
                    iterator.next();
                    iterator.remove();
                }
                getYoutrackAPI().command(issue.getTicket(), Fields.assignee.getCommand() + " " + login, null, null, disableNotifications, null);
            }
            final IssueStatus status = issue.getStatus();
            if (status != null) {
                StateValues ticketState = null;
                if (IssueStatus.REOPENED.equals(status)) {
                    ticketState = StateValues.Reopened;
                } else if (IssueStatus.RESOLVED.equals(status)) {
                    ticketState = StateValues.Fixed;
                } else if (IssueStatus.TO_RESOLVE.equals(status)) {
                    ticketState = StateValues.Open;
                } else if (IssueStatus.CLOSED.equals(status)) {
                    ticketState = StateValues.Verified;
                }
                if (ticketState != null) {
                    getYoutrackAPI().command(issue.getTicket(), Command.stateCommand(ticketState));
                }
            }
            final IssuePriority issuePriority = issue.getPriority();
            if (issuePriority != null) {
                getYoutrackAPI().command(issue.getTicket(), getYoutrackPriorityField() + " " + issuePriority.getName(), null, null, disableNotifications, null);
            }
            final StringBuilder command = new StringBuilder();
            if (issue.getLineStart() > -1) {
                command.append(" ").append(getYoutrackLineStartField()).append(" ").append(issue.getLineStart());
            }
            if (issue.getLineEnd() > -1) {
                command.append(" ").append(getYoutrackLineEndField()).append(" ").append(issue.getLineEnd());
            }
            if (command.length() > 0) {
                getYoutrackAPI().command(issue.getTicket(), command.toString(), null, null, disableNotifications, null);
            }
            final String vcsRev = issue.getVcsRev();
            if (!StringUtils.isBlank(vcsRev)) {
                getYoutrackAPI().command(issue.getTicket(), getYoutrackVcsRevField() + " " + vcsRev, null, null, disableNotifications, null);
            }
            final VirtualFile file = issue.getFile();
            if (file != null) {
                final String path = getRelativePath(file);
                getYoutrackAPI().command(issue.getTicket(), getYoutrackFileField() + " " + path, null, null, disableNotifications, null);
            }
        }
    }
}
