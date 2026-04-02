package de.audi.app.terminalmode.smartphone.androidauto2.nav;

import de.audi.app.terminalmode.IContext;
import de.audi.app.terminalmode.smartphone.androidauto2.AbstractAndroidAuto2Handler;
import de.audi.app.terminalmode.statemachine.Application;
import de.audi.app.terminalmode.statemachine.ApplicationOwner;
import de.audi.app.terminalmode.statemachine.IStateHandler;
import de.audi.atip.log.LogChannel;
import org.dsi.ifc.androidauto2.DSIAndroidAuto2;

/**
 * NavActiveIgnore (Audi): matches community navignore_audi.jar / reference bytecode.
 * Stock lsd.jar notifies DSI on native focus; this variant skips dsi.navFocusNotification(1, true)
 * for the NATIVE branch and replaces projected-nav request path with updateNavFocus(DEVICE).
 */
public class AndroidAuto2NavHandler extends AbstractAndroidAuto2Handler implements IAndroidAuto2NavHandler {

    private static String LOGCLASS;

    private volatile ApplicationOwner appNaviOwnerCurrent;

    private volatile boolean hasNavFocusRequested;

    private volatile ApplicationOwner expectedAppNavOwner;

    public AndroidAuto2NavHandler(LogChannel logChannel, DSIAndroidAuto2 dSIAndroidAuto2, IStateHandler iStateHandler, IContext iContext) {
        super(logChannel, dSIAndroidAuto2, iStateHandler, iContext);
        this.appNaviOwnerCurrent = ApplicationOwner.NOBODY;
        this.expectedAppNavOwner = ApplicationOwner.NOBODY;
    }

    public void updateNavFocus(ApplicationOwner applicationOwner) {
        if (this.hasNavFocusRequested) {
            if (this.expectedAppNavOwner.equals(applicationOwner)) {
                this.logger.log(1078071040, "[%1.updateNavFocus] responding to requested navFocusNotification: %2", (Object) "AndroidAuto2NavHandler", (Object) (ApplicationOwner.DEVICE.equals(applicationOwner) ? "PROJECTED" : "NATIVE"));
                this.dsi.navFocusNotification(ApplicationOwner.DEVICE.equals(applicationOwner) ? 2 : 1, false);
                this.hasNavFocusRequested = false;
                this.expectedAppNavOwner = ApplicationOwner.NOBODY;
            }
        } else {
            if (this.appNaviOwnerCurrent.equals(applicationOwner)) {
                this.logger.log(1078071040, "[%1.updateNavFocus] %2 -> %3, do nothing", (Object) "AndroidAuto2NavHandler", (Object) this.appNaviOwnerCurrent, (Object) applicationOwner);
                return;
            }
            if (ApplicationOwner.DEVICE.equals(applicationOwner)) {
                this.logger.log(1078071040, "[%1.updateNavFocus] %2 -> %3, navFocusNotification: PROJECTED", (Object) "AndroidAuto2NavHandler", (Object) this.appNaviOwnerCurrent, (Object) applicationOwner);
                this.dsi.navFocusNotification(2, true);
            } else {
                this.logger.log(1078071040, "[%1.updateNavFocus] %2 -> %3, navFocusNotification: NATIVE", (Object) "AndroidAuto2NavHandler", (Object) this.appNaviOwnerCurrent, (Object) applicationOwner);
            }
        }
        this.appNaviOwnerCurrent = applicationOwner;
    }

    public void navFocusRequestNotification(int n, int n2) {
        if (this.isValid(n2)) {
            this.logger.log(1078071040, "<- [%1.navFocusRequestNotification] %2", (Object) "AndroidAuto2NavHandler", (Object) this.getNavState(n));
            if (1 == n) {
                this.requestDSIUpdate(Application.NAVI, ApplicationOwner.NOBODY);
                this.hasNavFocusRequested = true;
                this.expectedAppNavOwner = ApplicationOwner.NOBODY;
            } else if (2 == n) {
                this.hasNavFocusRequested = true;
                this.expectedAppNavOwner = ApplicationOwner.DEVICE;
                this.updateNavFocus(ApplicationOwner.DEVICE);
            }
        }
    }

    /**
     * Shaped like reference navignore_audi.jar: lookupswitch + chained StringBuffer.append (no temp local).
     */
    private String getNavState(int n) {
        switch (n) {
            case 1:
                return "NAV_NATIVE";
            case 2:
                return "NAV_PROJECTED";
            default:
                return new StringBuffer().append("NAV_UNKNOWN").append(n).toString();
        }
    }

    protected String getLogClass() {
        return "AndroidAuto2NavHandler";
    }
}
