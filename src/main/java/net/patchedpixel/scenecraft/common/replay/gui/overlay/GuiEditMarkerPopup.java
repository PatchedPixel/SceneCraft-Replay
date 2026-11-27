package net.patchedpixel.scenecraft.common.replay.gui.overlay;

import com.google.common.base.Strings;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.patchedpixel.scenecraft.common.scenestudio.data.Marker;
import net.patchedpixel.scenecraft.common.flamex.screen.FlameableScreen;
import net.patchedpixel.scenecraft.common.flamex.FlameX;
import net.patchedpixel.scenecraft.common.flamex.widget.NumberBox;

import java.util.function.Consumer;

public class GuiEditMarkerPopup extends FlameableScreen {
    private final Consumer<Marker> onSave;

    public final EditBox nameField = FlameX.textField(150, "", null, null);
    // TODO: Replace with a min/sec/msec field
    public final NumberBox timeField = new NumberBox(150, 0);
    public final NumberBox xField = new NumberBox(150, 10);
    public final NumberBox yField = new NumberBox(150, 10);
    public final NumberBox zField = new NumberBox(150, 10);
    public final NumberBox yawField = new NumberBox(150, 5);
    public final NumberBox pitchField = new NumberBox(150, 5);
    public final NumberBox rollField = new NumberBox(150, 5);

    public GuiEditMarkerPopup(Screen parent, Marker marker, Consumer<Marker> onSave) {
        super("scenecraft.gui.editkeyframe.title.marker", parent, 5);
        this.panel = true;
        this.dim = true;
        this.onSave = onSave;
        nameField.setValue(Strings.nullToEmpty(marker.getName()));
        timeField.setNumber(marker.getTime());
        xField.setNumber(marker.getX());
        yField.setNumber(marker.getY());
        zField.setNumber(marker.getZ());
        yawField.setNumber(marker.getYaw());
        pitchField.setNumber(marker.getPitch());
        rollField.setNumber(marker.getRoll());
    }

    @Override
    protected void build(LinearLayout root) {
        GridLayout grid = new GridLayout().columnSpacing(7).rowSpacing(3);
        GridLayout.RowHelper rows = grid.createRowHelper(2);
        row(rows, "scenecraft.gui.editkeyframe.markername", nameField);
        row(rows, "scenecraft.gui.editkeyframe.timestamp", timeField);
        row(rows, "scenecraft.gui.editkeyframe.xpos", xField);
        row(rows, "scenecraft.gui.editkeyframe.ypos", yField);
        row(rows, "scenecraft.gui.editkeyframe.zpos", zField);
        row(rows, "scenecraft.gui.editkeyframe.camyaw", yawField);
        row(rows, "scenecraft.gui.editkeyframe.campitch", pitchField);
        row(rows, "scenecraft.gui.editkeyframe.camroll", rollField);
        root.addChild(grid);

        LinearLayout buttons = FlameX.row(7);
        buttons.addChild(FlameX.button("scenecraft.gui.save", 150, this::save));
        buttons.addChild(FlameX.button("scenecraft.gui.cancel", 150, this::close));
        root.addChild(buttons);
    }

    private void row(GridLayout.RowHelper rows, String labelKey, EditBox field) {
        StringWidget label = FlameX.label(labelKey);
        rows.addChild(label);
        rows.addChild(field);
    }

    private void save() {
        Marker marker = new Marker();
        marker.setName(Strings.emptyToNull(nameField.getValue()));
        marker.setTime(timeField.getInteger());
        marker.setX(xField.getDouble());
        marker.setY(yField.getDouble());
        marker.setZ(zField.getDouble());
        marker.setYaw(yawField.getFloat());
        marker.setPitch(pitchField.getFloat());
        marker.setRoll(rollField.getFloat());
        onSave.accept(marker);
        close();
    }
}
