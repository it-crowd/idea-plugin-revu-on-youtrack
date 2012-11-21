package pl.itcrowd.revu.youtrack.actions.review;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.project.Project;
import pl.itcrowd.revu.youtrack.business.YoutrackReviewManager;

public class RefreshYoutrackReviewAction extends AnAction {
// -------------------------- OTHER METHODS --------------------------

    @Override
    public void actionPerformed(AnActionEvent e)
    {
        Project project = e.getData(DataKeys.PROJECT);
        if (project == null) {
            return;
        }
        final YoutrackReviewManager youtrackReview = project.getComponent(YoutrackReviewManager.class);
        try {
            youtrackReview.refreshReview();
        } catch (Exception ex) {
            YoutrackReviewManager.handleException(YoutrackReviewManager.REFRESH_REVIEW_ERROR, ex, project);
        }
    }
}
