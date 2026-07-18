package listener;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import service.OrderScheduler;

@WebListener
public class AppStartupListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        OrderScheduler.start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        OrderScheduler.stop();
    }
}
