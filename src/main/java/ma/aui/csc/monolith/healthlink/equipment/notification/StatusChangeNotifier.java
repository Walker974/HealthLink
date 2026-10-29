package ma.aui.csc.monolith.healthlink.equipment.notification;

public interface StatusChangeNotifier {

    void statusChanged(EquipmentStatusChanged event);
}
