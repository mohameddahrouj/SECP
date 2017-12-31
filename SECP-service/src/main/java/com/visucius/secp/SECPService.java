package com.visucius.secp;

import com.visucius.secp.Chat.ChatServlet;
import com.visucius.secp.Chat.ChatSocketCreator;
import com.visucius.secp.Controllers.Admin.AdminController;
import com.visucius.secp.Controllers.GroupController;
import com.visucius.secp.Controllers.User.LoginRequestController;
import com.visucius.secp.Controllers.TokenController;
import com.visucius.secp.Controllers.User.UserController;
import com.visucius.secp.Controllers.User.UserRegistrationController;
import com.visucius.secp.auth.SECPAuthenticator;
import com.visucius.secp.auth.SECPAuthorizer;
import com.visucius.secp.auth.TokenAuthFilter;
import com.visucius.secp.config.*;
import com.visucius.secp.daos.*;
import com.visucius.secp.models.*;
import com.visucius.secp.resources.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletRegistration;


public class SECPService extends Application<SECPConfiguration> {
    private static final Logger LOG = LoggerFactory.getLogger(SECPService.class);

    public static void main(String[] args) throws Exception {
        new SECPService().run(args);
    }

    private final HibernateBundle<SECPConfiguration> hibernateBundle = new HibernateBundle<SECPConfiguration>(

            User.class,
            Message.class,
            Role.class,
            Group.class,
            Permission.class,
            Void.class
        ) {
        @Override
        public DataSourceFactory getDataSourceFactory(SECPConfiguration configuration) {
            return configuration.getDataSourceFactory();
        }
    };

    @Override
    public String getName() {
        return "SECP";
    }

    @Override
    public void initialize(Bootstrap<SECPConfiguration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/assets/app/", "/", "index.html"));
        bootstrap.addBundle(new AssetsBundle("/assets/app", "/login", "index.html", "login"));
        bootstrap.addBundle(new AssetsBundle("/assets/app", "/register", "index.html", "register"));
        bootstrap.addBundle(new AssetsBundle("/assets/app", "/chats", "index.html", "chats"));
        bootstrap.addBundle(new AssetsBundle("/assets/app", "/portal", "index.html", "portal"));
        bootstrap.addBundle(new AssetsBundle("/assets/app", "/portal/audit", "index.html", "audit"));
        bootstrap.addBundle(new AssetsBundle("/assets/app", "/portal/audit/user", "index.html", "audit-user"));
        bootstrap.addBundle(new AssetsBundle("/assets/app", "/portal/audit/group", "index.html", "audit-group"));
        bootstrap.addBundle(new AssetsBundle("/assets/app", "/portal/manage", "index.html", "manage"));
        bootstrap.addBundle(new AssetsBundle("/assets/app", "/portal/manage/user", "index.html", "manage-user"));
        bootstrap.addBundle(new AssetsBundle("/assets/app", "/portal/manage/group", "index.html", "manage-group"));
        bootstrap.addBundle(new AssetsBundle("/assets/app", "/error/404", "index.html", "404"));

        bootstrap.addBundle(hibernateBundle);
        ObjectMapper mapper = bootstrap.getObjectMapper();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void run(SECPConfiguration configuration,
                    Environment environment) throws Exception {
        environment.jersey().setUrlPattern("/SECP/*");


        //********************** Register DAO *************************************************
        final UserDAO userDAO =  new UserDAO(hibernateBundle.getSessionFactory());
        final GroupDAO groupDAO = new GroupDAO(hibernateBundle.getSessionFactory());
        final RolesDAO rolesDAO = new RolesDAO(hibernateBundle.getSessionFactory());
        final PermissionDAO permissionDAO = new PermissionDAO(hibernateBundle.getSessionFactory());


        //********************** Register Services/Controllers *********************************
        final UserRegistrationController userRegistrationController = new UserRegistrationController(userDAO);
        final TokenController tokenController = new TokenController(configuration);
        final LoginRequestController loginRequestController = new LoginRequestController(tokenController, userDAO);
        final UserController userController = new UserController(userDAO);
        final AdminController adminController = new AdminController(userDAO);
        final GroupController groupController = new GroupController(groupDAO,userDAO,rolesDAO, permissionDAO);


        //********************** Register authentication for User *****************************

        environment.jersey().register(RolesAllowedDynamicFeature.class);
        SECPAuthenticator authenticator = new UnitOfWorkAwareProxyFactory(hibernateBundle)
            .create(SECPAuthenticator.class,
                new Class<?>[]  { UserDAO.class, TokenController.class},
                new Object[]    { userDAO, tokenController});


        final TokenAuthFilter<User> tokenAuthFilter = new TokenAuthFilter.Builder<User>()
            .setAuthorizer(new SECPAuthorizer())
            .setAuthenticator(authenticator).buildAuthFilter();
        environment.jersey().register(new AuthDynamicFeature(tokenAuthFilter));
        environment.jersey().register(new AuthValueFactoryProvider.Binder<>(User.class));


        //************************** Group Resources *************************************
        environment.jersey().register(
            new GroupResource(groupController));

        //************************** Registering Resources *************************************
        environment.jersey().register(
            new EntryResource(userRegistrationController, loginRequestController, userController));
        environment.jersey().register(new AdminResource(adminController));

        //************************** Error Handling *************************************
        final ErrorPageErrorHandler epeh = new ErrorPageErrorHandler();
        epeh.addErrorPage(404, "/error/404");
        environment.getApplicationContext().setErrorHandler(epeh);

        //************************** WebSocket Servlet *************************************
        ChatSocketCreator chatSocketCreator = new UnitOfWorkAwareProxyFactory(hibernateBundle)
            .create(ChatSocketCreator.class,
                new Class<?>[]  { UserDAO.class},
                new Object[]    { userDAO});
        ServletRegistration.Dynamic webSocket = environment.servlets().addServlet("ws", new ChatServlet(chatSocketCreator));
        webSocket.setAsyncSupported(true);
        webSocket.addMapping("/chat/*");
    }
}
