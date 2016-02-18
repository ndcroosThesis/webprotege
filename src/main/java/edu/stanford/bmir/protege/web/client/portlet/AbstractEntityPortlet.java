package edu.stanford.bmir.protege.web.client.portlet;

import com.google.common.base.Optional;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.place.shared.PlaceChangeEvent;
import com.google.gwt.user.client.ui.*;
import com.google.web.bindery.event.shared.Event;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.gwtext.client.core.EventObject;
import com.gwtext.client.core.Function;
import com.gwtext.client.core.Position;
import com.gwtext.client.widgets.*;
import com.gwtext.client.widgets.Button;
import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.TabPanel;
import com.gwtext.client.widgets.event.ButtonListenerAdapter;
import com.gwtext.client.widgets.layout.FitLayout;
import edu.stanford.bmir.protege.web.client.LoggedInUserProvider;
import edu.stanford.bmir.protege.web.client.events.UserLoggedInEvent;
import edu.stanford.bmir.protege.web.client.events.UserLoggedInHandler;
import edu.stanford.bmir.protege.web.client.events.UserLoggedOutEvent;
import edu.stanford.bmir.protege.web.client.events.UserLoggedOutHandler;
import edu.stanford.bmir.protege.web.client.ui.util.AbstractValidatableTab;
import edu.stanford.bmir.protege.web.client.ui.util.ValidatableTab;
import edu.stanford.bmir.protege.web.shared.event.HasEventHandlerManagement;
import edu.stanford.bmir.protege.web.shared.event.PermissionsChangedEvent;
import edu.stanford.bmir.protege.web.shared.event.PermissionsChangedHandler;
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

/**
 * @deprecated Use {@link AbstractOWLEntityPortlet}.
 */
@Deprecated
public abstract class AbstractEntityPortlet implements EntityPortlet, HasEventHandlerManagement {

    private final SelectionModel selectionModel;

    private final EventBus eventBus;

    private final LoggedInUserProvider loggedInUserProvider;

    private List<HandlerRegistration> handlerRegistrations = new ArrayList<>();

    private final ProjectId projectId;

    private final PortletUi portletUi = new PortletUiImpl();

    @Inject
    public AbstractEntityPortlet(SelectionModel selectionModel,
                                 EventBus eventBus,
                                 LoggedInUserProvider loggedInUserProvider,
                                 ProjectId projectId) {
        this(selectionModel, eventBus, projectId, loggedInUserProvider);
    }

    private AbstractEntityPortlet(SelectionModel selectionModel, EventBus eventBus, ProjectId projectId, LoggedInUserProvider loggedInUserProvider) {
        super();
        this.selectionModel = selectionModel;
        this.eventBus = eventBus;
        this.loggedInUserProvider = loggedInUserProvider;
        this.projectId = projectId;

        addApplicationEventHandler(UserLoggedInEvent.TYPE, new UserLoggedInHandler() {
            @Override
            public void handleUserLoggedIn(UserLoggedInEvent event) {
                onLogin(event.getUserId());
            }
        });

        addApplicationEventHandler(UserLoggedOutEvent.TYPE, new UserLoggedOutHandler() {
            @Override
            public void handleUserLoggedOut(UserLoggedOutEvent event) {
                onLogout(event.getUserId());
            }
        });

        addProjectEventHandler(PermissionsChangedEvent.TYPE, new PermissionsChangedHandler() {
            @Override
            public void handlePersmissionsChanged(PermissionsChangedEvent event) {
                onPermissionsChanged();
            }
        });

        addApplicationEventHandler(PlaceChangeEvent.TYPE, new PlaceChangeEvent.Handler() {
            @Override
            public void onPlaceChange(PlaceChangeEvent event) {
                commitChanges();
            }
        });

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

    /*
     * Tools methods
     */
    protected Tool[] getTools() {
        // create some shared tools
        Tool refresh = new Tool(Tool.REFRESH, new Function() {
            public void execute() {
                onRefresh();
            }
        });
        Tool gear = new Tool(Tool.GEAR, new Function() {
            public void execute() {
                onConfigure();
            }
        });

        Tool close = new Tool(Tool.CLOSE, new Function() {
            public void execute() {
                onClose();
            }
        });

        List<Tool> toolList = new ArrayList<Tool>();
        if (hasRefreshButton()) {
            toolList.add(refresh);
        }
        toolList.add(gear);
        toolList.add(close);
        return toolList.toArray(new Tool[toolList.size()]);
    }

    protected boolean hasRefreshButton() {
        return true;
    }

    protected void onRefresh() {
    }

    public void commitChanges() {
    }

    protected void onConfigure() {
        final Window window = new Window();
        window.setSize(550, 250);
        window.setTitle("Configure");
        window.setLayout(new FitLayout());
        window.setClosable(true);
        window.setPaddings(7);
        window.setCloseAction(Window.CLOSE);
        final TabPanel configPanel = getConfigurationPanel();
        window.add(configPanel);
        window.setButtonAlign(Position.CENTER);

        window.addButton(new Button("OK", new ButtonListenerAdapter() {
            @Override
            public void onClick(Button button, EventObject e) {
                if (isValidConfiguration(configPanel)) {
                    saveConfiguration(configPanel);
                    window.close();
                }
                else {
                    MessageBox.alert("Invalid configuration", "The configuration is not valid. Please fix it, and try again");
                }
            }
        }));

        window.addButton(new Button("Cancel", new ButtonListenerAdapter() {
            @Override
            public void onClick(Button button, EventObject e) {
                window.close();
            }
        }));

        window.show();
    }

    protected boolean isValidConfiguration(TabPanel configPanel) {
        Component[] tabs = configPanel.getComponents();
        for (int i = 0; i < tabs.length; i++) {
            Component comp = tabs[i];
            if (comp instanceof ValidatableTab) {
                ValidatableTab tab = (ValidatableTab) comp;
                if (!tab.isValid()) {
                    return false;
                }
            }
        }
        return true;
    }

    protected void saveConfiguration(TabPanel configPanel) {
        Component[] tabs = configPanel.getComponents();
        for (int i = 0; i < tabs.length; i++) {
            Component comp = tabs[i];
            if (comp instanceof ValidatableTab) {
                ValidatableTab tab = (ValidatableTab) comp;
                tab.onSave();
            }
        }
    }

    protected TabPanel getConfigurationPanel() {
        TabPanel tabPanel = new TabPanel();
        tabPanel.add(createGeneralConfigPanel());
        return tabPanel;
    }

    protected Panel createGeneralConfigPanel() {

        Panel generalPanel = new AbstractValidatableTab() {
            @Override
            public void onSave() {
            }

            @Override
            public boolean isValid() {
                return true;
            }
        };

        generalPanel.setTitle("General");
        generalPanel.setPaddings(10);

        return generalPanel;
    }


    protected void onClose() {
        commitChanges();
//        this.hide();
//        this.destroy();
    }

    protected void onLogin(UserId userId) {
    }

    protected void onLogout(UserId userId) {
    }

    public void onPermissionsChanged() {
    }

//    /**
//     * Does not do anything to the UI, only stores the new configuration in this
//     * portlet
//     * @param portletConfiguration
//     */
//    public void setPortletConfiguration(PortletConfiguration portletConfiguration) {
//        this.portletConfiguration = portletConfiguration;
//    }
//
//    public PortletConfiguration getPortletConfiguration() {
//        return portletConfiguration;
//    }

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


    public boolean isEventForThisProject(Event<?> event) {
        Object source = event.getSource();
        return source instanceof ProjectId && source.equals(getProjectId());
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


    private int assignedHeight = 0;

    @Override
    public void setHeight(int height) {
//        super.setHeight(height);
        this.assignedHeight = height;
    }

    /**
     * If the portlet hasn't been rendered then the call to super.getHeight can return funny values.  Just return
     * the assigned height.
     * @return The height
     */
    @Override
    public int getHeight() {
        return assignedHeight;
//        int height = super.getHeight();
//        if(height <= 0) {
//            return assignedHeight;
//        }
//        else {
//            return height;
//        }
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

    @Override
    public void fireEvent(GwtEvent<?> gwtEvent) {
        handlerManager.fireEvent(gwtEvent);
    }

    @Override
    public Widget asWidget() {
        return portletUi.asWidget();
    }
}