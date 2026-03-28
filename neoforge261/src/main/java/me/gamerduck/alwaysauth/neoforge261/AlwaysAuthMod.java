package me.gamerduck.alwaysauth.neoforge261;

import net.neoforged.fml.common.Mod;

@Mod(AlwaysAuthMod.MODID)
public class AlwaysAuthMod {
    public static final String MODID = "alwaysauth";

    public AlwaysAuthMod() {
    }


//    @SubscribeEvent
//    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
//        if (event.getEntity().permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.ADMINS))) {
//            neoForgePlatform.getUpdateMessage().ifPresent(msg -> ((ServerPlayer) event.getEntity()).sendSystemMessage(Component.literal(msg)));
//        }
//    }

}