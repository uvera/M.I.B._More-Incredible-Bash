package de.audi.app.car.sdis.comp;

import de.audi.app.car.MenuCtrl;
import de.audi.app.car.sdis.base.AbstractDSICarDrivingCharacteristics;
import de.audi.app.car.sdis.base.IDSIObserver;
import de.audi.app.car.sdis.base.ISDISFramework;
import de.audi.app.car.sdis.base.IVisibility;
import de.audi.atip.log.LogChannel;
import de.esolutions.fw.comm.asi.hmisync.car.driving.TADConfiguration;
import de.esolutions.fw.comm.asi.hmisync.car.driving.impl.ASIHMISyncCarDrivingAbstractBaseService;
import org.dsi.ifc.base.DSIBase;
import org.dsi.ifc.base.DSIListener;
import org.dsi.ifc.cardrivingcharacteristics.CharismaViewOptions;
import org.dsi.ifc.cardrivingcharacteristics.DSICarDrivingCharacteristics;
import org.dsi.ifc.cardrivingcharacteristics.SuspensionControlAdditionalFunctions;
import org.dsi.ifc.cardrivingcharacteristics.SuspensionControlViewOptions;
import org.dsi.ifc.cardrivingcharacteristics.TADVehicleInfo;
import org.dsi.ifc.cardrivingcharacteristics.TADViewOptions;

/**
 * M.I.B. Sport HMI: appends {@code CAR_FUNCTION_SPORT_HMI} (52) to {@link #CODING} so
 * {@link de.audi.app.car.sdis.base.AbstractCarStateHandler#registerCarStates} runs for Sport
 * (menu visibility / clamp-speed-standstill updates). Inner {@link CarStateHandler} handles
 * state id 52 without calling non-existent ASI sport hooks (stock default was log-only).
 *
 * Decompiled baseline + local edits; rebuild against your train's {@code lsd.jar}.
 */
public class CarDrivingCharacteristicsComponent extends AbstractDSICarDrivingCharacteristics implements IDSIObserver {

    public static final byte[] CODING;
    private static final int[] attributes;

    private final LogChannel logger;
    private DSICarDrivingCharacteristics dsi;
    private ISDISFramework baseService;
    private CarStateHandler carStateHandler;
    private ASIHMISyncCarDrivingAbstractBaseService toSDIS;

    static Class class$org$dsi$ifc$cardrivingcharacteristics$DSICarDrivingCharacteristics;
    static Class class$org$dsi$ifc$cardrivingcharacteristics$DSICarDrivingCharacteristicsListener;

    public CarDrivingCharacteristicsComponent(ISDISFramework iSDISFramework) {
        super(iSDISFramework.getLogChannel("App.CarSDIS.CarDrivingCharacteristics"));
        this.logger = iSDISFramework.getLogChannel("App.CarSDIS.CarDrivingCharacteristics");
        this.baseService = iSDISFramework;
        this.toSDIS = this.baseService.getASIDataUpdater().getDrivingASI();
        this.carStateHandler = new CarStateHandler(this, this.logger);
    }

    static Class class$(String string) {
        try {
            return Class.forName(string);
        } catch (ClassNotFoundException e) {
            throw new NoClassDefFoundError(e.getMessage());
        }
    }

    public void init() {
        this.notCodedInfo();
        this.carStateHandler.registerCarStates(CODING);
        /* Stock never calls updateVisibility(..., 52): no DSI sport VO callback. Seed SDIS/menu
         * pipeline once with a visible CarViewOption like Charisma/TAD do from DSI. */
        try {
            int vis = this.baseService.updateVisibility(MenuCtrl.VIS_VO, (short) 52);
            this.carStateHandler.setSportHmiVisibility(vis);
        } catch (Throwable t) {
            this.logger.log(10000, 4090, t);
        }
        Class dsiCl = class$org$dsi$ifc$cardrivingcharacteristics$DSICarDrivingCharacteristics;
        if (dsiCl == null) {
            dsiCl = class$("org.dsi.ifc.cardrivingcharacteristics.DSICarDrivingCharacteristics");
            class$org$dsi$ifc$cardrivingcharacteristics$DSICarDrivingCharacteristics = dsiCl;
        }
        Class lisCl = class$org$dsi$ifc$cardrivingcharacteristics$DSICarDrivingCharacteristicsListener;
        if (lisCl == null) {
            lisCl = class$("org.dsi.ifc.cardrivingcharacteristics.DSICarDrivingCharacteristicsListener");
            class$org$dsi$ifc$cardrivingcharacteristics$DSICarDrivingCharacteristicsListener = lisCl;
        }
        this.baseService.registerDSI(dsiCl.getName(), lisCl.getName(), (IDSIObserver) this);
    }

    public void deinit() {
        Class dsiCl = class$org$dsi$ifc$cardrivingcharacteristics$DSICarDrivingCharacteristics;
        if (dsiCl == null) {
            dsiCl = class$("org.dsi.ifc.cardrivingcharacteristics.DSICarDrivingCharacteristics");
            class$org$dsi$ifc$cardrivingcharacteristics$DSICarDrivingCharacteristics = dsiCl;
        }
        this.baseService.deRegisterDSI(dsiCl.getName());
        this.carStateHandler.deregisterCarStates();
    }

    public void notCodedInfo() {
        int n = 0;
        do {
            if (this.carStateHandler.isActivated((short) CODING[n])) {
                continue;
            }
            switch (CODING[n]) {
                case 40:
                    this.sendUpdateTADVisibilityState(0);
                    break;
                case 21:
                    this.sendUpdateSuspensionVisibilityState(new int[] {0, 0});
                    break;
                case 17:
                    this.sendUpdateCharismaVisibilityState(0);
                    break;
                case 52:
                    break;
                default:
                    this.logger.log(-2137614336, 15686, (long) CODING[n]);
            }
        } while (++n < CODING.length);
    }

    public void updateTADVehicleInfo(TADVehicleInfo tADVehicleInfo) {
        de.esolutions.fw.comm.asi.hmisync.car.driving.TADVehicleInfo tADVehicleInfo2 =
                new de.esolutions.fw.comm.asi.hmisync.car.driving.TADVehicleInfo();
        tADVehicleInfo2.setRoofLoad(tADVehicleInfo.isRoofLoad());
        tADVehicleInfo2.setTrailer(tADVehicleInfo.isTrailer());
        try {
            this.toSDIS.updateTADVehicleInfo(tADVehicleInfo2);
        } catch (Throwable methodException) {
            this.logger.log(10000, 4076, methodException);
        }
    }

    public void updateTADCurrentRollAngle(float f, int n) {
        if (n != 1) {
            return;
        }
        this.logger.log(-2137614336, 15687, (Object) String.valueOf(f));
        float f2 = f;
        if (f2 == 16711494) {
            f2 = 0.0f;
        }
        try {
            this.toSDIS.updateTADCurrentRollAngle(f2);
        } catch (Throwable methodException) {
            this.logger.log(10000, 4077, methodException);
        }
    }

    public void updateTADPosMaxRollAngle(float f, int n) {
        if (n != 1) {
            return;
        }
        this.logger.log(-2137614336, 15688, (Object) String.valueOf(f));
        try {
            this.toSDIS.updateTADPosMaxRollAngle(f);
        } catch (Throwable methodException) {
            this.logger.log(10000, 4078, methodException);
        }
    }

    public void updateTADNegMaxRollAngle(float f, int n) {
        if (n != 1) {
            return;
        }
        this.logger.log(-2137614336, 15689, (Object) String.valueOf(f));
        try {
            this.toSDIS.updateTADNegMaxRollAngle(f);
        } catch (Throwable methodException) {
            this.logger.log(10000, 4079, methodException);
        }
    }

    public void updateTADCurrentPitchAngle(float f, int n) {
        if (n != 1) {
            return;
        }
        this.logger.log(-2137614336, 15690, (Object) String.valueOf(f));
        float f2 = f;
        if (f2 == 16711494) {
            f2 = 0.0f;
        }
        try {
            this.toSDIS.updateTADCurrentPitchAngle(f2);
        } catch (Throwable methodException) {
            this.logger.log(10000, 4080, methodException);
        }
    }

    public void updateTADPosMaxPitchAngle(float f, int n) {
        if (n != 1) {
            return;
        }
        this.logger.log(-2137614336, 15691, (Object) String.valueOf(f));
        try {
            this.toSDIS.updateTADPosMaxPitch(f);
        } catch (Throwable methodException) {
            this.logger.log(10000, 4081, methodException);
        }
    }

    public void updateTADNegMaxPitchAngle(float f, int n) {
        if (n != 1) {
            return;
        }
        this.logger.log(-2137614336, 15692, (Object) String.valueOf(f));
        try {
            this.toSDIS.updateTADNegMaxPitch(f);
        } catch (Throwable methodException) {
            this.logger.log(10000, 4082, methodException);
        }
    }

    public void updateTADViewOptions(TADViewOptions tADViewOptions, int n) {
        if (n != 1) {
            return;
        }
        this.logger.log(-2137614336, 15693, (Object) tADViewOptions);
        this.sendUpdateTADVConfiguration(tADViewOptions.getConfiguration());
        int n2 = this.baseService.updateVisibility(tADViewOptions.getAngleDisplay(), (short) 40);
        this.carStateHandler.setTadVisibility(n2);
        this.sendUpdateTADVisibilityState(n2);
    }

    private void sendUpdateTADVisibilityState(int n) {
        try {
            this.logger.log(-2137614336, 15694, (Object) IVisibility.TEXT_TO_STATE[n]);
            this.toSDIS.updateTADVisibilityState(n);
        } catch (Throwable methodException) {
            this.logger.log(10000, 4083, methodException);
        }
    }

    private void sendUpdateTADVConfiguration(org.dsi.ifc.cardrivingcharacteristics.TADConfiguration tADConfiguration) {
        if (tADConfiguration != null) {
            TADConfiguration tADConfiguration2 = new TADConfiguration();
            tADConfiguration2.setRollAngleStartSoftWarning(tADConfiguration.getRollAngleStartSoftWarning());
            tADConfiguration2.setRollAngleStartHardWarning(tADConfiguration.getRollAngleStartHardWarning());
            tADConfiguration2.setRollAngleMaxScale(tADConfiguration.getRollAngleMaxScale());
            tADConfiguration2.setPitchAngleStartSoftWarning(tADConfiguration.getPitchAngleStartSoftWarning());
            tADConfiguration2.setPitchAngleStartHardWarning(tADConfiguration.getPitchAngleStartHardWarning());
            tADConfiguration2.setPitchAngleMaxScale(tADConfiguration.getPitchAngleMaxScale());
            tADConfiguration2.setRollAngleInstallation(tADConfiguration.isRollAngleInstallation());
            tADConfiguration2.setPitchAngleInstallation(tADConfiguration.isPitchAngleInstallation());
            try {
                this.logger.log(-2137614336, 15695, (Object) tADConfiguration);
                this.toSDIS.updateTADConfiguration(tADConfiguration2);
            } catch (Throwable methodException) {
                this.logger.log(10000, 4084, methodException);
            }
        }
    }

    public void updateSuspensionControlCurrentLevel(int n, int n2) {
        if (n2 != 1) {
            return;
        }
        this.logger.log(-2137614336, 15696, (long) n);
        try {
            this.toSDIS.updateSuspensionControlCurrentLevel(n);
        } catch (Throwable methodException) {
            this.logger.log(10000, 4085, methodException);
        }
    }

    public void updateSuspensionControlTargetLevel(int n, int n2) {
        if (n2 != 1) {
            return;
        }
        this.logger.log(-2137614336, 15697, (long) n);
        try {
            this.toSDIS.updateSuspensionControlTargetLevel(n);
        } catch (Throwable methodException) {
            this.logger.log(10000, 4086, methodException);
        }
    }

    public void updateSuspensionControlViewOptions(SuspensionControlViewOptions suspensionControlViewOptions, int n) {
        int n2;
        if (n != 1) {
            return;
        }
        boolean bl = this.baseService.getCarAdaption().isMenuDisplayActivated((short) 21);
        if (!bl) {
            this.logger.log(-2137614336, 15698, (long) this.baseService.getCarAdaption().getByteCoding((short) 40));
            return;
        }
        this.logger.log(-2137614336, 15699, (Object) suspensionControlViewOptions);
        boolean bl2 = this.isAirSuspensionLvlIconVisible(suspensionControlViewOptions);
        if (bl2) {
            n2 = 2;
            this.carStateHandler.setSuspVisibility(2);
        } else {
            n2 = 1;
            this.carStateHandler.setSuspVisibility(1);
        }
        this.sendUpdateSuspensionVisibilityState(new int[] {n2, n2});
    }

    private boolean isAirSuspensionLvlIconVisible(SuspensionControlViewOptions suspensionControlViewOptions) {
        if (suspensionControlViewOptions.getConfiguration() != null
                && suspensionControlViewOptions.getConfiguration().getAdditionalFunctionsAvailability() != null) {
            SuspensionControlAdditionalFunctions suspensionControlAdditionalFunctions =
                    suspensionControlViewOptions.getConfiguration().getAdditionalFunctionsAvailability();
            return suspensionControlAdditionalFunctions.isActualNiveau();
        }
        return false;
    }

    private void sendUpdateSuspensionVisibilityState(int[] nArray) {
        try {
            this.logger.log(-2137614336, 15700, (Object) IVisibility.TEXT_TO_STATE[nArray[0]]);
            this.toSDIS.updateSuspensionVisibilityState(nArray);
        } catch (Throwable methodException) {
            this.logger.log(10000, 4087, methodException);
        }
    }

    public void updateCharismaViewOptions(CharismaViewOptions charismaViewOptions, int n) {
        if (n != 1) {
            return;
        }
        this.logger.log(-2137614336, 15701, (Object) charismaViewOptions.getActiveProfile());
        int n2 = this.baseService.updateVisibility(charismaViewOptions.getActiveProfile(), (short) 17);
        this.carStateHandler.setCharismaVisibility(n2);
        this.sendUpdateCharismaVisibilityState(n2);
    }

    private void sendUpdateCharismaVisibilityState(int n) {
        try {
            this.logger.log(-2137614336, 15702, (Object) IVisibility.TEXT_TO_STATE[n]);
            this.toSDIS.updateDriveSelectActiveProfileVisibilityState(n);
        } catch (Throwable methodException) {
            this.logger.log(10000, 4088, methodException);
        }
    }

    public void updateCharismaActiveProfile(int n, int n2) {
        if (n2 != 1) {
            return;
        }
        try {
            this.logger.log(-2137614336, 15703, (long) n);
            this.toSDIS.updateDriveSelectActiveProfile(n);
        } catch (Throwable methodException) {
            this.logger.log(10000, 4089, methodException);
        }
    }

    public void setDSI(DSIBase dSIBase) {
        this.dsi = (DSICarDrivingCharacteristics) dSIBase;
        this.dsi.setNotification(attributes, (DSIListener) this);
    }

    final class CarStateHandler extends de.audi.app.car.sdis.base.AbstractCarStateHandler {
        private volatile int tadVisibility;
        private volatile int suspVisibility;
        private volatile int charismaVisibility;
        private volatile int sportHmiVisibility;

        CarStateHandler(CarDrivingCharacteristicsComponent outer, LogChannel logChannel) {
            super(logChannel, outer.baseService);
        }

        void setTadVisibility(int v) {
            this.tadVisibility = v;
        }

        void setSuspVisibility(int v) {
            this.suspVisibility = v;
        }

        void setCharismaVisibility(int v) {
            this.charismaVisibility = v;
        }

        void setSportHmiVisibility(int v) {
            this.sportHmiVisibility = v;
        }

        public void updateClampState(boolean bl, boolean bl2, boolean bl3, boolean bl4) {
            super.updateClampState(bl, bl2, bl3, bl4);
            this.logger.log(1078071040, 7406, bl2);
            this.updateVisibilityAfterCarStateChange((short) 40, this.tadVisibility);
            this.updateVisibilityAfterCarStateChange((short) 21, this.suspVisibility);
            this.updateVisibilityAfterCarStateChange((short) 17, this.charismaVisibility);
            this.updateVisibilityAfterCarStateChange((short) 52, this.sportHmiVisibility);
        }

        public void exceedsUpperThreshold(int n) {
            super.exceedsUpperThreshold(n);
            this.logger.log(1078071040, 7407, this.isUnderVThr);
            this.updateVisibilityAfterCarStateChange((short) 40, this.tadVisibility);
            this.updateVisibilityAfterCarStateChange((short) 21, this.suspVisibility);
            this.updateVisibilityAfterCarStateChange((short) 17, this.charismaVisibility);
            this.updateVisibilityAfterCarStateChange((short) 52, this.sportHmiVisibility);
        }

        public void belowLowerThreshold(int n) {
            super.belowLowerThreshold(n);
            this.logger.log(1078071040, 7408, this.isUnderVThr);
            this.updateVisibilityAfterCarStateChange((short) 40, this.tadVisibility);
            this.updateVisibilityAfterCarStateChange((short) 21, this.suspVisibility);
            this.updateVisibilityAfterCarStateChange((short) 17, this.charismaVisibility);
            this.updateVisibilityAfterCarStateChange((short) 52, this.sportHmiVisibility);
        }

        public void updateStandStill(boolean bl) {
            super.updateStandStill(bl);
            this.logger.log(1078071040, 7409, bl);
            this.updateVisibilityAfterCarStateChange((short) 40, this.tadVisibility);
            this.updateVisibilityAfterCarStateChange((short) 21, this.suspVisibility);
            this.updateVisibilityAfterCarStateChange((short) 17, this.charismaVisibility);
            this.updateVisibilityAfterCarStateChange((short) 52, this.sportHmiVisibility);
        }

        private void updateVisibilityAfterCarStateChange(short s, int n) {
            int n2 = this.updateMenuEntryVisibility(s, n);
            if (s == 40) {
                CarDrivingCharacteristicsComponent.this.sendUpdateTADVisibilityState(n2);
            } else if (s == 21) {
                CarDrivingCharacteristicsComponent.this.sendUpdateSuspensionVisibilityState(new int[] {n2, n2});
            } else if (s == 17) {
                CarDrivingCharacteristicsComponent.this.sendUpdateCharismaVisibilityState(n2);
            } else if (s == 52) {
                /* No ASIHMISyncCarDriving sport hook on this train; menu side uses SDIS visibility. */
            } else {
                this.logger.log(-1601830656, 1982, (long) s);
            }
        }
    }

    static {
        CODING = new byte[] {40, 21, 17, 52};
        attributes = new int[] {23, 22, 27, 25, 26, 24, 21, 19, 1, 30, 29, 13, 12};
    }
}
