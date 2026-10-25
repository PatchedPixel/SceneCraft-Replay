package net.patchedpixel.scenecraft.gui.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.METHOD, ElementType.PARAMETER})
public @interface NonNull {
}
