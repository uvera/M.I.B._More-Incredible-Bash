/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  de.audi.atip.base.IFrameworkAccess
 *  de.audi.atip.hmi.modelaccess.ChoiceModelApp
 *  de.audi.atip.log.LogChannel
 *  de.audi.atip.sysapp.SysConst
 *  de.audi.tghu.car.app.CarModelBank
 *  de.esolutions.fw.util.commons.Buffer
 *  org.dsi.ifc.global.CarViewOption
 */
package de.audi.app.car;

import de.audi.atip.base.IFrameworkAccess;
import de.audi.atip.hmi.modelaccess.ChoiceModelApp;
import de.audi.atip.log.LogChannel;
import de.audi.atip.sysapp.SysConst;
import de.audi.tghu.car.app.CarModelBank;
import de.esolutions.fw.util.commons.Buffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import org.dsi.ifc.global.CarViewOption;

public final class MenuCtrl {
    /* Stock bytecode never assigns these in <clinit>; JVM default is 0. CFR left them bare; javac needs = 0. */
    public static final int STATE_FUNCTIONAL = 0;
    public static final int STATE_INVISIBLE = 0;
    public static final int STATE_DISABLED_ERROR = 0;
    public static final int STATE_DISABLED_CLAMP15 = 0;
    public static final int STATE_DISABLED_SPEED = 0;
    public static final int STATE_DISABLED_ENGINE = 0;
    public static final int STATE_DISABLED_CURRENT_ERROR = 0;
    public static final int STATE_DISABLED_TIME_NO_MODIFY = 0;
    public static final int STATE_DISABLED_DRVSEL_EFFIENCY_NOT_POSSIBLE = 0;
    public static final int STATE_DISABLED_VIN_NOT_DETECTED = 0;
    public static final int UPDATE_EVENT_CL15 = 0;
    public static final int UPDATE_EVENT_VTHR = 0;
    public static final int UPDATE_EVENT_DSI = 0;
    public static final int UPDATE_EVENT_CMOF_RELOAD = 0;
    public static final int CAR_MODEL_TYPE_C7_AVANT_AU572 = 0;
    public static final int CAR_MODEL_TYPE_C7_LIMO_AU571 = 0;
    public static final int CAR_MODEL_TYPE_C7_COUPE_AU573 = 0;
    public static final int CAR_MODEL_TYPE_C7_ALLROAD_AU576 = 0;
    public static final int CAR_MODEL_TYPE_C7_ALLROAD_AU5724 = 0;
    public static final int CAR_MODEL_TYPE_A3_AU370 = 0;
    public static final int CAR_MODEL_TYPE_A3_SPORTBACK_AU373 = 0;
    public static final int CAR_MODEL_TYPE_A3_CABRIO_AU375 = 0;
    public static final int CAR_MODEL_TYPE_A3_LIMO_AU371 = 0;
    public static final int CAR_MODEL_TYPE_RSETRON_AU9147 = 0;
    static final String LOG_CH = "";
    private static IFrameworkAccess framework;
    public static volatile boolean stsClamp15On;
    public static boolean stsVehicleMoving;
    public static boolean speedBelowBoardbookBrowserThreshold;
    public static boolean stsStandstill;
    private static boolean menuFlagsRead;
    private static byte[] menuFlag;
    public static LogChannel logCh;
    private static Map mapMenuItemStatePriority;
    public static final CarViewOption INVIS_VO;
    public static final CarViewOption VIS_VO;
    public static final CarViewOption NOT_ACCESSIBLE_VO;
    public static String[] menuTxt;

    public static int getMenuItemStatePriority(int n) {
        if (mapMenuItemStatePriority == null) {
            mapMenuItemStatePriority = new HashMap();
            mapMenuItemStatePriority.put(new Integer(1), new Integer(0));
            mapMenuItemStatePriority.put(new Integer(2), new Integer(1));
            mapMenuItemStatePriority.put(new Integer(3), new Integer(2));
            mapMenuItemStatePriority.put(new Integer(5), new Integer(3));
            mapMenuItemStatePriority.put(new Integer(4), new Integer(4));
            mapMenuItemStatePriority.put(new Integer(6), new Integer(5));
            mapMenuItemStatePriority.put(new Integer(0), new Integer(6));
        }
        if (n > 6) {
            n = 6;
        }
        return ((Integer) mapMenuItemStatePriority.get(new Integer(n))).intValue();
    }

    public static int ddbOnOff(boolean bl) {
        return bl ? 0 : 1;
    }

    public static boolean isCoded(int n) {
        /* CAR main menu: Sport row uses initMenuEntryOnCarCoding(..., n) -> isCoded; n is the menu/CAR slot
         * for SPORTHMI (52, same as CAR_FUNCTION_SPORT_HMI / menuTxt index). CMOF menuFlag[52] can stay off
         * even when adaptation 52 is set. Do not use IMenuCoding.MFLG_SPORTHMI here: several lsd.jar builds
         * leave interface static finals at JVM default 0, which would wrongly match only slot 0. */
        if (n == 52) {
            return true;
        }
        int n2 = n != -1 ? menuFlag[n] : 7;
        return (n2 & 1) == 1;
    }

    public static byte getFlag(int n) {
        return menuFlag[n];
    }

    public static void setFramework(IFrameworkAccess iFrameworkAccess) {
        framework = iFrameworkAccess;
        logCh = framework.getLogChannel("MenuCtrl.CES");
    }

    public static IFrameworkAccess getFramework() {
        return framework;
    }

    public static boolean loadProcessor(int n) {
        return n == -1 || MenuCtrl.isCoded(n);
    }

    public static boolean isDSIsimulated(String string) {
        StringBuffer stringBuffer = new StringBuffer(System.getProperty("SimulateFSG", "false"));
        return stringBuffer.indexOf("all") != -1 || stringBuffer.indexOf(string) != -1;
    }

    public static void readFlags() {
        int n;
        if (menuFlagsRead) {
            return;
        }
        if (framework == null) {
            System.out.println("MenuCtrl.readFlags: fw = NULL, exited");
            return;
        }
        MenuCtrl.setCarModelType();
        byte[] byArray = framework.getSysApp().getCarMenuFlags();
        if (byArray != null && byArray.length >= 56) {
            logCh.log(1078071040, 65, (long)0, (long)byArray.length);
            menuFlag = byArray;
        } else if (byArray != null) {
            logCh.log(10000, 106, (long)0, (long)byArray.length);
            for (n = 0; n < byArray.length; ++n) {
                MenuCtrl.menuFlag[n] = byArray[n];
            }
        } else {
            logCh.log(10000, 107);
        }
        n = 0;
        int n2 = 0;
        while (n2 < menuFlag.length) {
            n += menuFlag[n2];
            int n3 = n2++;
            menuFlag[n3] = (byte)(menuFlag[n3] + 32);
        }
        if (n == 0 || n == -1 * menuFlag.length) {
            logCh.log(1078071040, 66);
            System.err.println("All CMOF are 0 or -1, take special coding from lsd.sh SetMenuCoding");
            MenuCtrl.setMenuCoding();
        } else {
            logCh.log(1078071040, 67);
        }
        n2 = 0;
        while (n2 < menuFlag.length) {
            int n4 = n2++;
            menuFlag[n4] = (byte)(menuFlag[n4] & 0xF);
        }
        if (logCh.isInfo()) {
            logCh.log(1078071040, 68);
            logCh.log(1078071040, 69);
            logCh.log(1078071040, 70);
            for (n2 = 0; n2 < 56; ++n2) {
                logCh.log(1078071040, 71, (Object)MenuCtrl.menuVisibility(n2, true));
            }
            for (n2 = 56; n2 < menuFlag.length; ++n2) {
                logCh.log(1078071040, 72, (long)n2, (long)menuFlag[n2]);
            }
            logCh.log(1078071040, 73);
        }
        menuFlagsRead = true;
        MenuCtrl.initSystemModels();
        MenuCtrl.initSysConst();
    }

    private static void initSysConst() {
        int[] nArray = SysConst.getSysConstData();
        int n = MenuCtrl.isCoded(50) ? 1 : 0;
        Integer ij = Integer.getInteger("EOLFLAG_DISPLAY_ECALL");
        nArray[119] = ij != null ? ij.intValue() : n;
        int n2 = MenuCtrl.isCoded(51) ? 1 : 0;
        Integer ij2 = Integer.getInteger("EOLFLAG_DISPLAY_ENI");
        nArray[120] = ij2 != null ? ij2.intValue() : n2;
    }

    private static void initSystemModels() {
        framework.getSysApp().getChoiceModelApp(35).setValue(MenuCtrl.isCoded(17) ? 1 : 0);
        if (MenuCtrl.isCoded(20) || MenuCtrl.isCoded(23)) {
            framework.getSysApp().getChoiceModelApp(277).setValue(2);
        } else {
            framework.getSysApp().getChoiceModelApp(277).setValue(1);
        }
    }

    private static void setMenuCoding() {
        String string = System.getProperty("SetMenuCoding", "").toUpperCase().trim();
        StringTokenizer stringTokenizer = new StringTokenizer(string, ",:");
        while (stringTokenizer.hasMoreTokens()) {
            String string2 = stringTokenizer.nextToken();
            byte by = (byte)(Integer.parseInt(stringTokenizer.nextToken()) & 0xFF);
            if (string2.equalsIgnoreCase("ALL")) {
                if (logCh.isInfo()) {
                    logCh.log(1078071040, new StringBuffer().append("setMenuCoding: set all codings to ").append(by).toString());
                }
                Arrays.fill(menuFlag, by);
                return;
            }
            for (int i = 0; i < menuTxt.length; ++i) {
                if (!menuTxt[i].equalsIgnoreCase(string2)) continue;
                if (logCh.isInfo()) {
                    logCh.log(1078071040, new StringBuffer().append("setMenuCoding: set ").append(menuTxt[i]).append(" to ").append(by).toString());
                }
                MenuCtrl.menuFlag[i] = by;
            }
        }
    }

    private static void deactivateFSGs(String string) {
        for (int i = 0; i < 56; ++i) {
            String string2 = menuTxt[i];
            if (string.indexOf(string2) == -1) continue;
            MenuCtrl.menuFlag[i] = 0;
        }
    }

    public static String getMenuFlagsList() {
        Buffer buffer = new Buffer();
        for (int i = 0; i < 56; ++i) {
            buffer.append(MenuCtrl.menuVisibility(i, true)).append('\n');
        }
        return buffer.toString();
    }

    public static void setCarModelType() {
        byte by = framework.getSysApp().getDiagnosisCOD().getCarClass();
        byte by2 = framework.getSysApp().getDiagnosisCOD().getCarGeneration();
        byte by3 = framework.getSysApp().getDiagnosisCOD().getCarDerivate();
        byte by4 = framework.getSysApp().getDiagnosisCOD().getCarDerivateSupplement();
        logCh.log(1078071040, 74, (long)by, (long)by2, (long)by3);
        logCh.log(1078071040, 75, (long)by4);
        if (by == 3) {
            if (by2 == 7) {
                switch (by3) {
                    case 0: {
                        framework.getSysApp().getChoiceModelApp(27).setValue(23);
                        break;
                    }
                    case 1: {
                        framework.getSysApp().getChoiceModelApp(27).setValue(26);
                        break;
                    }
                    case 3: {
                        framework.getSysApp().getChoiceModelApp(27).setValue(24);
                        break;
                    }
                    case 5: {
                        framework.getSysApp().getChoiceModelApp(27).setValue(25);
                        break;
                    }
                    default: {
                        logCh.log(-2137614336, 183, (long)by3);
                        framework.getSysApp().getChoiceModelApp(27).setValue(23);
                        break;
                    }
                }
            } else {
                logCh.log(-2137614336, 184, (long)by2);
                framework.getSysApp().getChoiceModelApp(27).setValue(23);
            }
        } else if (by == 5) {
            if (by2 == 7) {
                switch (by3) {
                    case 1: {
                        framework.getSysApp().getChoiceModelApp(27).setValue(10);
                        break;
                    }
                    case 2: {
                        if (by4 == 4) {
                            framework.getSysApp().getChoiceModelApp(27).setValue(18);
                            break;
                        }
                        framework.getSysApp().getChoiceModelApp(27).setValue(9);
                        break;
                    }
                    case 3: 
                    case 4: {
                        framework.getSysApp().getChoiceModelApp(27).setValue(12);
                        break;
                    }
                    case 6: {
                        framework.getSysApp().getChoiceModelApp(27).setValue(18);
                        break;
                    }
                    default: {
                        logCh.log(-2137614336, 185, (long)by3);
                        framework.getSysApp().getChoiceModelApp(27).setValue(10);
                        break;
                    }
                }
            } else {
                logCh.log(-2137614336, 186, (long)by2);
                framework.getSysApp().getChoiceModelApp(27).setValue(10);
            }
        } else if (by == 9 && by2 == 1 && by3 == 4 && by4 == 7) {
            framework.getSysApp().getChoiceModelApp(27).setValue(27);
        } else {
            logCh.log(-2137614336, 187, (long)by);
            framework.getSysApp().getChoiceModelApp(27).setValue(23);
        }
    }

    public static void setMenuFlag(int n, byte by) {
        if (0 <= n && n < menuFlag.length) {
            MenuCtrl.menuFlag[n] = by;
        }
    }

    public static String menuVisibility(int n, boolean bl) {
        byte by = menuFlag[n];
        String string = "                                                     ";
        String string2 = bl ? "                  " : "";
        Buffer buffer = new Buffer().append("Menu ");
        buffer.append(menuTxt[n]).append(' ');
        buffer.append(bl ? string.substring(0, 25 - buffer.length()) : "");
        buffer.append('[');
        if (by < 10) {
            buffer.append(' ');
        }
        buffer.append((int)by).append("]: ");
        if ((by & 1) == 0) {
            buffer.append("decoded");
        } else if ((by & 7) == 7) {
            buffer.append("always");
        } else {
            buffer.append((by & 2) != 2 ? "clamp15 ON req.   " : string2);
            buffer.append((by & 4) != 4 ? "v < thr req.      " : string2);
            buffer.append((by & 8) == 8 ? "standstill        " : string2);
        }
        return buffer.toString();
    }

    public static void setAvailableChoice(ChoiceModelApp choiceModelApp, CarViewOption carViewOption, byte by) {
        if ((by & 1) == 0 || carViewOption.getState() == 0) {
            choiceModelApp.setValue(1);
            return;
        }
        int n = MenuCtrl.getAvailabilityOnCarState(by);
        if (carViewOption.getState() == 2 && n == 0) {
            logCh.log(-2137614336, 188, (long)choiceModelApp.getID());
            choiceModelApp.setValue(0);
            return;
        }
        MenuCtrl.setMenuAvailability(choiceModelApp, carViewOption.getReason(), n);
    }

    public static void setMainAvailableChoice(ChoiceModelApp choiceModelApp, CarViewOption[] carViewOptionArray, byte by) {
        MenuCtrl.setAvailableChoice(choiceModelApp, MenuCtrl.getSummary(carViewOptionArray), by);
    }

    public static void setMenuAvailability(ChoiceModelApp choiceModelApp, int n, int n2) {
        block0 : switch (n2) {
            case 1: {
                choiceModelApp.setValue(3);
                break;
            }
            case 2: {
                switch (n) {
                    case 2: {
                        choiceModelApp.setValue(3);
                        break block0;
                    }
                    case 1: {
                        choiceModelApp.setValue(2);
                        break block0;
                    }
                    case 4: {
                        choiceModelApp.setValue(5);
                        break block0;
                    }
                }
                choiceModelApp.setValue(4);
                break;
            }
            default: {
                switch (n) {
                    case 2: {
                        choiceModelApp.setValue(3);
                        break block0;
                    }
                    case 5: {
                        choiceModelApp.setValue(3);
                        break block0;
                    }
                    case 3: {
                        choiceModelApp.setValue(4);
                        break block0;
                    }
                    case 4: {
                        choiceModelApp.setValue(5);
                        break block0;
                    }
                    case 8: {
                        choiceModelApp.setValue(11);
                        break block0;
                    }
                    case 11: {
                        choiceModelApp.setValue(12);
                        break block0;
                    }
                    case 10: {
                        choiceModelApp.setValue(10);
                        break block0;
                    }
                    case 9: {
                        choiceModelApp.setValue(2);
                        break block0;
                    }
                }
                choiceModelApp.setValue(2);
            }
        }
        logCh.log(-2137614336, 189, (long)choiceModelApp.getID(), (long)choiceModelApp.getValue());
    }

    public static int getAvailabilityOnCarState(byte by) {
        if ((by & 2) == 0 && !stsClamp15On) {
            return 1;
        }
        if ((by & 4) == 0 && stsVehicleMoving) {
            return 2;
        }
        if ((by & 8) == 8 && !stsStandstill) {
            return 2;
        }
        return 0;
    }

    public static CarViewOption getSummary(CarViewOption[] carViewOptionArray) {
        int n = 0;
        int n2 = 0;
        int n3 = 0;
        int n4 = 0;
        int n5 = 0;
        int n6 = 0;
        CarViewOption carViewOption = new CarViewOption();
        for (int i = 0; i < carViewOptionArray.length; ++i) {
            switch (carViewOptionArray[i].getState()) {
                case 2: {
                    ++n2;
                    break;
                }
                case 0: {
                    ++n;
                    break;
                }
            }
            switch (carViewOptionArray[i].getReason()) {
                case 1: {
                    ++n3;
                    break;
                }
                case 2: 
                case 5: {
                    ++n4;
                    break;
                }
                case 4: {
                    ++n5;
                    break;
                }
                case 3: {
                    ++n6;
                    break;
                }
                default: {
                    carViewOption.reason = carViewOptionArray[i].getReason();
                }
            }
            if (carViewOptionArray.length != 15) continue;
            logCh.log(14808325, 7, (Object)carViewOptionArray[i]);
        }
        if (carViewOptionArray.length == 15) {
            logCh.log(14808325, 8, (long)n6);
        }
        carViewOption.state = n == carViewOptionArray.length ? 0 : (n2 > 0 ? 2 : 1);
        carViewOption.reason = carViewOption.state == 1 ? (n6 > 0 ? 3 : (n5 > 0 ? 4 : (n4 > 0 ? 2 : (n3 > 0 ? 1 : 0)))) : 0;
        return carViewOption;
    }

    public static CarViewOption getSummaryViewOption(CarModelBank carModelBank, int[] nArray) {
        logCh.log(-2137614336, 190, (Object)nArray);
        CarViewOption[] carViewOptionArray = new CarViewOption[nArray.length];
        for (int i = 0; i < nArray.length; ++i) {
            carViewOptionArray[i] = MenuCtrl.getViewOptionOfSubAvailableModel((ChoiceModelApp)carModelBank.getModel(nArray[i]));
        }
        CarViewOption carViewOption = MenuCtrl.getSummary(carViewOptionArray);
        logCh.log(-2137614336, 191, (Object)carViewOption);
        return carViewOption;
    }

    public static CarViewOption getViewOptionOfSubAvailableModel(ChoiceModelApp choiceModelApp) {
        int n;
        int n2;
        int n3 = choiceModelApp.getValue();
        switch (n3) {
            case 0: {
                n2 = 2;
                break;
            }
            case 6: {
                n2 = 2;
                break;
            }
            case 1: {
                n2 = 0;
                break;
            }
            case 2: {
                n2 = 1;
                break;
            }
            case 3: {
                n2 = 1;
                break;
            }
            case 4: {
                n2 = 1;
                break;
            }
            case 5: {
                n2 = 1;
                break;
            }
            default: {
                n2 = 1;
            }
        }
        switch (n3) {
            case 0: {
                n = 0;
                break;
            }
            case 1: {
                n = 0;
                break;
            }
            case 6: {
                n = 0;
                break;
            }
            case 2: {
                n = 1;
                break;
            }
            case 3: {
                n = 2;
                break;
            }
            case 4: {
                n = 3;
                break;
            }
            case 5: {
                n = 4;
                break;
            }
            default: {
                n = 1;
            }
        }
        CarViewOption carViewOption = new CarViewOption(n2, n);
        logCh.log(-2137614336, 192, (Object)carViewOption, (long)choiceModelApp.getID(), (long)n3);
        return carViewOption;
    }

    public static void initMenuEntryOnCarCoding(ChoiceModelApp choiceModelApp, int n) {
        int n2 = MenuCtrl.isCoded(n) ? 2 : 1;
        choiceModelApp.setValue(n2);
    }

    public static Object getVehicleStatusList() {
        Buffer buffer = new Buffer();
        buffer.append("Cl15:").append(stsClamp15On ? "ON" : "OFF");
        buffer.append(", ").append(stsVehicleMoving ? "MOVING" : "STANDSTILL");
        return buffer.toString();
    }

    public static void logTrackerEvent(String string, int n) {
        String string2 = "";
        String string3 = "";
        string2 = new StringBuffer().append("cl15 ").append(stsClamp15On ? "on" : "off").append(", car ").append(stsVehicleMoving ? "moving" : "stopped").toString();
        if (n == 0) {
            string3 = "CL15";
        }
        if (n == 1) {
            string3 = "vThr";
        }
        logCh.log(14808325, 9, (Object)string, (Object)string3, (Object)string2);
    }

    public static void reReadFlags() {
        logCh.log(14808325, 10);
        menuFlagsRead = false;
        MenuCtrl.readFlags();
    }

    static {
        stsStandstill = true;
        menuFlagsRead = false;
        menuFlag = new byte[56];
        INVIS_VO = new CarViewOption(0, 0);
        VIS_VO = new CarViewOption(2, 0);
        NOT_ACCESSIBLE_VO = new CarViewOption(1, 0);
        menuTxt = new String[]{"ACC", "INT_LIGHT", "PARKING", "AWV", "LDW", "SWA", "EXT_LIGHT", "WINDOW", "AIRCONDITION", "AUXHEATER", "BC_CLUSTER", "RDK", "WIPER", "SIA", "SEAT", "CENTRAL_LOCKING", "COMPASS", "CHARISMA", "OILLEVEL", "VIN", "CLOCK", "AIRSUSPENSION", "HUD", "UNITMASTER", "HYBRID", "UGDO", "NIGHTVISION", "SIDEVIEW", "RGS", "MFL_JOKER", "TSD", "ATTENTION_IDENT", "APTIVE_KEY_CLAMP", "MIRROR", "DRV_SCHOOL", "MKE", "BCME", "UNKNOWN_37", "BREAK", "START_STOP", "TILT_ANGLE_DISPLAY", "BATTERY_CTRL", "UNKNOWN_42", "UNKNOWN_43", "UNKNOWN_44", "UNKNOWN_45", "AUX_CLIMATE", "PEDESTRIAN_ASSIST", "SEAT_PNEUMATIC", "CURVE_ASSIST", "ECALL", "ENI", "SPORTHMI", "ADBLUE", "THINKBLUE", "EFFICIENCY_ASSIST"};
    }
}
