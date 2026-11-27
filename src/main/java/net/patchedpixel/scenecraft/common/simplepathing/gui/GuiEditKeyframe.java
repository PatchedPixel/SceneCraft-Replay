package net.patchedpixel.scenecraft.common.simplepathing.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.patchedpixel.scenecraft.common.pathing.properties.CameraProperties;
import net.patchedpixel.scenecraft.common.pathing.properties.TimestampProperty;
import net.patchedpixel.scenecraft.common.scenestudio.pathing.change.Change;
import net.patchedpixel.scenecraft.common.scenestudio.pathing.change.CombinedChange;
import net.patchedpixel.scenecraft.common.scenestudio.pathing.interpolation.CatmullRomSplineInterpolator;
import net.patchedpixel.scenecraft.common.scenestudio.pathing.interpolation.CubicSplineInterpolator;
import net.patchedpixel.scenecraft.common.scenestudio.pathing.interpolation.Interpolator;
import net.patchedpixel.scenecraft.common.scenestudio.pathing.interpolation.LinearInterpolator;
import net.patchedpixel.scenecraft.common.scenestudio.pathing.path.Keyframe;
import net.patchedpixel.scenecraft.common.scenestudio.pathing.path.Path;
import net.patchedpixel.scenecraft.common.scenestudio.pathing.path.PathSegment;
import net.patchedpixel.scenecraft.common.simplepathing.InterpolatorType;
import net.patchedpixel.scenecraft.common.simplepathing.SPTimeline;
import net.patchedpixel.scenecraft.common.simplepathing.SPTimeline.SPPath;
import net.patchedpixel.scenecraft.common.simplepathing.Setting;
import net.patchedpixel.scenecraft.common.simplepathing.properties.ExplicitInterpolationProperty;
import net.patchedpixel.scenecraft.common.flamex.screen.FlameableScreen;
import net.patchedpixel.scenecraft.common.flamex.FlameX;
import net.patchedpixel.scenecraft.common.flamex.widget.NumberBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;
import net.patchedpixel.scenecraft.common.flamex.widget.DropdownWidget;

import java.util.List;
import java.util.Optional;

public abstract class GuiEditKeyframe extends FlameableScreen {
    protected static final Logger logger = LogManager.getLogger();

    protected final GuiPathing guiPathing;

    protected final long time;
    protected final Keyframe keyframe;
    protected final Path path;

    public final NumberBox timeMinField = new NumberBox(30, 0).setRange(0, Double.POSITIVE_INFINITY);
    public final NumberBox timeSecField = new NumberBox(20, 0).setRange(0, 59);
    public final NumberBox timeMSecField = new NumberBox(30, 0).setRange(0, 999);

    private Button saveButton;

    public GuiEditKeyframe(GuiPathing gui, SPPath path, long time, String type) {
        super(Component.translatable("scenecraft.gui.editkeyframe.title." + type), MCVer.getMinecraft().screen, 10);
        this.panel = true;
        this.dim = true;
        this.background = Background.DEFAULT;
        this.guiPathing = gui;
        this.time = time;
        this.path = gui.getMod().getCurrentTimeline().getPath(path);
        this.keyframe = this.path.getKeyframe(time);

        timeMinField.setNumber((double) time / 1000 / 60);
        timeSecField.setNumber((double) time / 1000 % 60);
        timeMSecField.setNumber(time % 1000);
        timeMinField.setResponder(s -> updateSaveState());
        timeSecField.setResponder(s -> updateSaveState());
        timeMSecField.setResponder(s -> updateSaveState());

        this.saveAction = () -> {
            Change change = save();
            long newTime = newTime();
            if(newTime != time) {
                change = CombinedChange.createFromApplied(change,
                        gui.getMod().getCurrentTimeline().moveKeyframe(path, time, newTime));
                if(gui.getMod().getSelectedPath() == path && gui.getMod().getSelectedTime() == time) {
                    gui.getMod().setSelected(path, newTime);
                }
            }
            gui.getMod().getCurrentTimeline().getTimeline().pushChange(change);
            close();
        };
    }

    private final Runnable saveAction;

    protected long newTime() {
        return (timeMinField.getLong() * 60 + timeSecField.getLong()) * 1000 + timeMSecField.getLong();
    }

    protected void buildInputs(LinearLayout root) {}

    protected static LinearLayout timeRow(String labelKey, NumberBox min, NumberBox sec, NumberBox ms) {
        LinearLayout row = FlameX.row(3);
        row.addChild(FlameX.label(labelKey));
        row.addChild(min);
        row.addChild(FlameX.label("scenecraft.gui.minutes"));
        row.addChild(sec);
        row.addChild(FlameX.label("scenecraft.gui.seconds"));
        row.addChild(ms);
        row.addChild(FlameX.label("scenecraft.gui.milliseconds"));
        return row;
    }

    @Override
    protected void build(LinearLayout root) {
        buildInputs(root);
        root.addChild(timeRow("scenecraft.gui.editkeyframe.timelineposition", timeMinField, timeSecField, timeMSecField));

        LinearLayout buttons = FlameX.row(7);
        saveButton = FlameX.button("scenecraft.gui.save", 150, saveAction);
        buttons.addChild(saveButton);
        buttons.addChild(FlameX.button("scenecraft.gui.cancel", 150, this::close));
        root.addChild(buttons);
        updateSaveState();
    }

    private void updateSaveState() {
        if(saveButton != null) saveButton.active = canSave();
    }

    private boolean canSave() {
        long newTime = newTime();
        if(newTime < 0 || newTime > guiPathing.timeline.getLength()) {
            return false;
        }
        return newTime == keyframe.getTime() || path.getKeyframe(newTime) == null;
    }

    public void open() {
        display();
    }

    protected abstract Change save();

    public static class Spectator extends GuiEditKeyframe {
        public Spectator(GuiPathing gui, SPPath path, long keyframe) {
            super(gui, path, keyframe, "spec");
        }

        @Override
        protected Change save() {
            return CombinedChange.createFromApplied();
        }
    }

    public static class Time extends GuiEditKeyframe {
        public final NumberBox timestampMinField = new NumberBox(30, 0).setRange(0, Double.POSITIVE_INFINITY);
        public final NumberBox timestampSecField = new NumberBox(20, 0).setRange(0, 59);
        public final NumberBox timestampMSecField = new NumberBox(30, 0).setRange(0, 999);

        public Time(GuiPathing gui, SPPath path, long keyframe) {
            super(gui, path, keyframe, "time");

            this.keyframe.getValue(TimestampProperty.PROPERTY).ifPresent(time -> {
                timestampMinField.setNumber((double) time / 1000 / 60);
                timestampSecField.setNumber((double) time / 1000 % 60);
                timestampMSecField.setNumber(time % 1000);
            });
        }

        @Override
        protected void buildInputs(LinearLayout root) {
            root.addChild(timeRow("scenecraft.gui.editkeyframe.timestamp", timestampMinField, timestampSecField, timestampMSecField));
        }

        @Override
        protected Change save() {
            int time = (int) ((timestampMinField.getLong() * 60 + timestampSecField.getLong()) * 1000
                    + timestampMSecField.getLong());
            return guiPathing.getMod().getCurrentTimeline().updateTimeKeyframe(keyframe.getTime(), time);
        }
    }

    public static class Position extends GuiEditKeyframe {
        public final NumberBox xField = new NumberBox(60, 5);
        public final NumberBox yField = new NumberBox(60, 5);
        public final NumberBox zField = new NumberBox(60, 5);

        public final NumberBox yawField = new NumberBox(60, 5);
        public final NumberBox pitchField = new NumberBox(60, 5);
        public final NumberBox rollField = new NumberBox(60, 5);

        private InterpolatorType interpolatorType;
        private final boolean hasSegment;
        private final NumberBox alphaField = new NumberBox(100, 5).setRange(0, Double.POSITIVE_INFINITY);
        private DropdownWidget<InterpolatorType> typeButton;
        private StringWidget alphaLabel;

        public Position(GuiPathing gui, SPPath path, long keyframe) {
            super(gui, path, keyframe, "pos");

            this.keyframe.getValue(CameraProperties.POSITION).ifPresent(pos -> {
                xField.setNumber(pos.getLeft());
                yField.setNumber(pos.getMiddle());
                zField.setNumber(pos.getRight());
            });
            this.keyframe.getValue(CameraProperties.ROTATION).ifPresent(rot -> {
                yawField.setNumber(rot.getLeft());
                pitchField.setNumber(rot.getMiddle());
                rollField.setNumber(rot.getRight());
            });

            Optional<PathSegment> segment = this.path.getSegments().stream()
                    .filter(s -> s.getStartKeyframe() == this.keyframe).findFirst();
            hasSegment = segment.isPresent();
            interpolatorType = InterpolatorType.DEFAULT;
            if(segment.isPresent()) {
                Interpolator interpolator = segment.get().getInterpolator();
                InterpolatorType type = InterpolatorType.fromClass(interpolator.getClass());
                if(this.keyframe.getValue(ExplicitInterpolationProperty.PROPERTY).isPresent()) {
                    interpolatorType = type;
                } else {
                    type = InterpolatorType.DEFAULT;
                }
                if(typeNoDefault(type).getInterpolatorClass().isInstance(interpolator)) {
                    if(interpolator instanceof CatmullRomSplineInterpolator) {
                        alphaField.setNumber(((CatmullRomSplineInterpolator) interpolator).getAlpha());
                    }
                }
            }
        }

        private InterpolatorType typeNoDefault(InterpolatorType type) {
            if(type == InterpolatorType.DEFAULT || type == null) {
                return InterpolatorType.fromString(
                        guiPathing.getMod().getCore().getSettingsRegistry().get(Setting.DEFAULT_INTERPOLATION));
            }
            return type;
        }

        @Override
        protected void buildInputs(LinearLayout root) {
            GridLayout grid = new GridLayout();
            grid.columnSpacing(3).rowSpacing(5);
            String[] left = {"xpos", "ypos", "zpos"};
            NumberBox[] leftFields = {xField, yField, zField};
            String[] right = {"camyaw", "campitch", "camroll"};
            NumberBox[] rightFields = {yawField, pitchField, rollField};
            for(int i = 0; i < 3; i++) {
                grid.addChild(FlameX.label("scenecraft.gui.editkeyframe." + left[i]), i, 0,
                        grid.newCellSettings().alignHorizontallyRight().alignVerticallyMiddle());
                grid.addChild(leftFields[i], i, 1, grid.newCellSettings().alignVerticallyMiddle());
                grid.addChild(FlameX.label("scenecraft.gui.editkeyframe." + right[i]), i, 2,
                        grid.newCellSettings().alignHorizontallyRight().alignVerticallyMiddle());
                grid.addChild(rightFields[i], i, 3, grid.newCellSettings().alignVerticallyMiddle());
            }
            root.addChild(grid);

            typeButton = FlameX.dropdown(List.of(InterpolatorType.values()), interpolatorType,
                    t -> I18n.get(t.getI18nName()), t -> {
                        interpolatorType = t;
                        updateAlphaVisibility();
                    });
            typeButton.setEntryTooltip(t -> I18n.get(t.getI18nDescription()));
            typeButton.active = hasSegment;
            LinearLayout interpolator = FlameX.row(6);
            interpolator.addChild(FlameX.label("scenecraft.gui.editkeyframe.interpolator"));
            interpolator.addChild(typeButton);
            root.addChild(interpolator);

            alphaLabel = FlameX.label("scenecraft.gui.editkeyframe.interpolator.catmullrom.alpha");
            LinearLayout alpha = FlameX.row(0);
            alpha.addChild(alphaLabel);
            alpha.addChild(alphaField);
            root.addChild(alpha);
            updateAlphaVisibility();
        }

        private void updateAlphaVisibility() {
            boolean show = hasSegment && typeNoDefault(interpolatorType) == InterpolatorType.CATMULL_ROM;
            alphaField.visible = show;
            alphaLabel.visible = show;
        }

        private Interpolator createInterpolator() {
            return switch (typeNoDefault(interpolatorType)) {
                case CATMULL_ROM -> new CatmullRomSplineInterpolator(alphaField.getDouble());
                case CUBIC -> new CubicSplineInterpolator();
                default -> new LinearInterpolator();
            };
        }

        @Override
        protected Change save() {
            SPTimeline timeline = guiPathing.getMod().getCurrentTimeline();
            Change positionChange = timeline.updatePositionKeyframe(time,
                    xField.getDouble(), yField.getDouble(), zField.getDouble(),
                    yawField.getFloat(), pitchField.getFloat(), rollField.getFloat()
            );
            if(!hasSegment) {
                return positionChange;
            }
            Interpolator interpolator = createInterpolator();
            if(interpolatorType == InterpolatorType.DEFAULT) {
                return CombinedChange.createFromApplied(positionChange, timeline.setInterpolatorToDefault(time),
                        timeline.setDefaultInterpolator(interpolator));
            } else {
                return CombinedChange.createFromApplied(positionChange, timeline.setInterpolator(time, interpolator));
            }
        }
    }
}
