package edu.stanford.bmir.protege.web.client.portlet;

import com.google.common.base.Optional;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.Event;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import edu.stanford.bmir.protege.web.client.LoggedInUserProvider;
import edu.stanford.bmir.protege.web.client.events.UserLoggedInEvent;
import edu.stanford.bmir.protege.web.client.events.UserLoggedOutEvent;
import edu.stanford.bmir.protege.web.client.filter.FilterView;
import edu.stanford.bmir.protege.web.shared.event.HasEventHandlerManagement;
import edu.stanford.bmir.protege.web.shared.event.PermissionsChangedEvent;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import edu.stanford.bmir.protege.web.shared.selection.EntitySelectionChangedEvent;
import edu.stanford.bmir.protege.web.shared.selection.EntitySelectionChangedHandler;
import edu.stanford.bmir.protege.web.shared.selection.SelectionModel;
import edu.stanford.bmir.protege.web.shared.user.UserId;
import edu.stanford.protege.widgetmap.client.view.ViewTitleChangedEvent;
import edu.stanford.protege.widgetmap.client.view.ViewTitleChangedHandler;
import org.semanticweb.owlapi.model.OWLEntity;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;


public abstract class AbstractWebProtegePortlet implements WebProtegePortlet, HasEventHandlerManagement {

    private final SelectionModel selectionModel;

    private final EventBus eventBus;

    private final LoggedInUserProvider loggedInUserProvider;

    private List<HandlerRegistration> handlerRegistrations = new ArrayList<>();

    private final ProjectId projectId;

    private final PortletUi portletUi = new PortletUiImpl();

    @Inject
    public AbstractWebProtegePortlet(SelectionModel selectionModel,
                                     EventBus eventBus,
                                     LoggedInUserProvider loggedInUserProvider, ProjectId projectId) {
        this.selectionModel = selectionModel;
        this.eventBus = eventBus;
        this.loggedInUserProvider = loggedInUserProvider;
        this.projectId = projectId;

        addApplicationEventHandler(UserLoggedInEvent.TYPE, event -> handleLogin(event.getUserId()));

        addApplicationEventHandler(UserLoggedOutEvent.TYPE, event -> handleLogout(event.getUserId()));

        addProjectEventHandler(PermissionsChangedEvent.TYPE, event -> handlePermissionsChanged());

        HandlerRegistration handlerRegistration = selectionModel.addSelectionChangedHandler(new EntitySelectionChangedHandler() {
            @Override
            public void handleSelectionChanged(EntitySelectionChangedEvent event) {
                if (portletUi.asWidget().isAttached()) {
                    handleBeforeSetEntity(event.getPreviousSelection());
                    handleAfterSetEntity(event.getLastSelection());
                }
            }
        });
        handlerRegistrations.add(handlerRegistration);
        asWidget().addAttachHandler(event -> handleActivated());
    }


    @Override
    public void setToolbarVisible(boolean visible) {
        portletUi.setToolbarVisible(visible);
    }

    @Override
    public AcceptsOneWidget getContentHolder() {
        return portletUi.getContentHolder();
    }

    public SelectionModel getSelectionModel() {
        return selectionModel;
    }

    public void handleActivated() {
        handleAfterSetEntity(selectionModel.getSelection());
    }


    public ProjectId getProjectId() {
        return projectId;
    }

    public UserId getUserId() {
        return loggedInUserProvider.getCurrentUserId();
    }

    protected void handleBeforeSetEntity(Optional<OWLEntity> existingEntity) {
    }

    protected void handleAfterSetEntity(Optional<OWLEntity> entityData) {
    }

    protected void handleLogin(UserId userId) {
    }

    protected void handleLogout(UserId userId) {
    }

    public void handlePermissionsChanged() {
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////



    /**
     * Adds an event handler to the main event bus.  When the portlet is destroyed the handler will automatically be
     * removed.
     * @param type The type of event to listen for.  Not {@code null}.
     * @param handler The handler for the event type. Not {@code null}.
     * @param <T> The event type
     * @throws NullPointerException if any parameters are {@code null}.
     */
    public <T> void addProjectEventHandler(Event.Type<T> type, T handler) {
        HandlerRegistration reg = eventBus.addHandlerToSource(checkNotNull(type), getProjectId(), checkNotNull(handler));
        handlerRegistrations.add(reg);
    }

    public <T> void addApplicationEventHandler(Event.Type<T> type, T handler) {
        HandlerRegistration reg = eventBus.addHandler(checkNotNull(type), checkNotNull(handler));
        handlerRegistrations.add(reg);
    }

    private void removeHandlers() {
        for (HandlerRegistration reg : handlerRegistrations) {
            reg.removeHandler();
        }
    }

    public Optional<OWLEntity> getSelectedEntity() {
        return getSelectionModel().getSelection();
    }


    @Override
    public String toString() {
        return toStringHelper("EntityPortlet")
                .addValue(getClass().getName())
                .toString();
    }

    @Override
    public void dispose() {
        removeHandlers();
    }

    private String title = "";

    private HandlerManager handlerManager = new HandlerManager(this);

    public void setTitle(String title) {
        this.title = title;
        fireEvent(new ViewTitleChangedEvent(title));
    }

    @Override
    public com.google.gwt.event.shared.HandlerRegistration addViewTitleChangedHandler(ViewTitleChangedHandler viewTitleChangedHandler) {
        return handlerManager.addHandler(ViewTitleChangedEvent.getType(), viewTitleChangedHandler);
    }

    @Override
    public String getViewTitle() {
        return title;
    }

    @Override
    public void addPortletAction(PortletAction portletAction) {
        portletUi.addPortletAction(portletAction);
        portletUi.setToolbarVisible(true);
    }

    public void setFilter(FilterView filterView) {
        portletUi.setFilterView(filterView);
        portletUi.setToolbarVisible(true);
    }

    @Override
    public void fireEvent(GwtEvent<?> gwtEvent) {
        handlerManager.fireEvent(gwtEvent);
    }

    @Override
    public Widget asWidget() {
        return portletUi.asWidget();
    }
}