package dev.codex.altmanager.forge;

import dev.codex.altmanager.AccountKind;
import dev.codex.altmanager.AltManagerService;
import dev.codex.altmanager.MinecraftAccount;
import dev.codex.altmanager.auth.AccessTokenSanitizer;
import dev.codex.altmanager.auth.DeviceCodeSession;
import dev.codex.altmanager.session.SessionSwitchResult;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class AltManagerScreen extends Screen {
    private static final int PANEL = 0xC8202430;
    private static final int PANEL_DARK = 0xDD11151F;
    private static final int BORDER = 0xFF3B4254;
    private static final int ACCENT = 0xFF6AA9FF;
    private static final int SUCCESS = 0xFFA7E3A1;
    private static final int WARNING = 0xFFFFD27F;
    private static final int TEXT = 0xFFE8EDF7;
    private static final int MUTED = 0xFF9AA6BA;
    private static final int DANGER = 0xFFFF8E8E;

    private final Screen parent;
    private final AltManagerService service;
    private Component status;
    private boolean statusError;
    private String selectedUuid;
    private List<MinecraftAccount> accounts = new ArrayList<>();
    private EditBox tokenField;
    private boolean busy;
    private int listX;
    private int listY;
    private int listW;
    private int listBottom;
    private int rowH;
    private ScreenLayout layout;

    public AltManagerScreen(Screen parent) {
        this(parent, t("status.ready"), false, null);
    }

    private AltManagerScreen(Screen parent, Component status, boolean statusError, String selectedUuid) {
        super(t("screen.title"));
        this.parent = parent;
        this.status = status == null ? t("status.ready") : status;
        this.statusError = statusError;
        this.selectedUuid = selectedUuid;
        this.service = AltManagerForgeClient.service();
    }

    @Override
    protected void init() {
        reloadAccounts();
        ensureSelection();

        this.layout = createLayout();
        this.listX = layout.listX;
        this.listY = layout.listY;
        this.listW = layout.listW;
        this.listBottom = layout.listBottom;
        this.rowH = layout.rowH;

        MinecraftAccount selected = selectedAccount();

        tokenField = new EditBox(this.font, layout.contentX, layout.tokenY, layout.contentW, 20, t("field.access_token"));
        tokenField.setMaxLength(8192);
        tokenField.setHint(t("placeholder.access_token"));
        this.addRenderableWidget(tokenField);

        if (layout.compact) {
            initCompactButtons(selected);
            return;
        }

        Button useButton = button(t("button.use_selected"), layout.detailX, layout.actionY, layout.detailW, 20, b -> switchSelected());
        useButton.active = selected != null && !busy;
        this.addRenderableWidget(useButton);

        Button refreshButton = button(t("button.refresh"), layout.detailX, layout.actionY + 24, (layout.detailW - 6) / 2, 20, b -> refreshSelected());
        refreshButton.active = selected != null && selected.getKind() == AccountKind.MICROSOFT && !busy;
        this.addRenderableWidget(refreshButton);

        Button deleteButton = button(t("button.delete"), layout.detailX + (layout.detailW + 6) / 2, layout.actionY + 24, (layout.detailW - 6) / 2, 20, b -> deleteSelected());
        deleteButton.active = selected != null && !busy;
        this.addRenderableWidget(deleteButton);

        this.addRenderableWidget(button(t("button.back"), layout.contentX, layout.navY, 128, 20, b -> returnToParent()));
        this.addRenderableWidget(button(t("button.reload"), layout.contentX + 134, layout.navY, 118, 20,
                b -> refreshScreen(t("status.reloaded"), false, selectedUuid)));

        Button microsoftButton = button(t("button.microsoft"), layout.contentX + layout.contentW - 282, layout.navY, 132, 20, b -> loginMicrosoft());
        microsoftButton.active = !busy;
        this.addRenderableWidget(microsoftButton);

        Button importButton = button(t("button.import_token"), layout.contentX + layout.contentW - 144, layout.navY, 144, 20, b -> loginAccessToken());
        importButton.active = !busy;
        this.addRenderableWidget(importButton);
    }

    private void initCompactButtons(MinecraftAccount selected) {
        int gap = 4;
        int useW = Math.max(92, (layout.contentW * 42) / 100);
        int sideW = Math.max(64, (layout.contentW - useW - gap * 2) / 2);
        if (useW + sideW * 2 + gap * 2 > layout.contentW) {
            sideW = Math.max(54, (layout.contentW - useW - gap * 2) / 2);
        }

        Button useButton = button(t("button.use_short"), layout.contentX, layout.actionY, useW, 20, b -> switchSelected());
        useButton.active = selected != null && !busy;
        this.addRenderableWidget(useButton);

        Button refreshButton = button(t("button.refresh_short"), layout.contentX + useW + gap, layout.actionY, sideW, 20, b -> refreshSelected());
        refreshButton.active = selected != null && selected.getKind() == AccountKind.MICROSOFT && !busy;
        this.addRenderableWidget(refreshButton);

        Button deleteButton = button(t("button.delete"), layout.contentX + useW + sideW + gap * 2, layout.actionY, layout.contentW - useW - sideW - gap * 2, 20, b -> deleteSelected());
        deleteButton.active = selected != null && !busy;
        this.addRenderableWidget(deleteButton);

        int navW = (layout.contentW - gap * 3) / 4;
        this.addRenderableWidget(button(t("button.back"), layout.contentX, layout.navY, navW, 20, b -> returnToParent()));
        this.addRenderableWidget(button(t("button.reload"), layout.contentX + navW + gap, layout.navY, navW, 20,
                b -> refreshScreen(t("status.reloaded"), false, selectedUuid)));

        Button microsoftButton = button(t("button.microsoft_short"), layout.contentX + (navW + gap) * 2, layout.navY, navW, 20, b -> loginMicrosoft());
        microsoftButton.active = !busy;
        this.addRenderableWidget(microsoftButton);

        Button importButton = button(t("button.import_short"), layout.contentX + (navW + gap) * 3, layout.navY, layout.contentW - (navW + gap) * 3, 20, b -> loginAccessToken());
        importButton.active = !busy;
        this.addRenderableWidget(importButton);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ScreenLayout currentLayout = this.layout == null ? createLayout() : this.layout;
        graphics.fill(0, 0, this.width, this.height, 0xF0101117);
        graphics.fill(0, 0, this.width, 26, 0xFF151A24);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 9, TEXT);

        if (currentLayout.compact) {
            renderCompact(graphics, mouseX, mouseY, currentLayout);
        } else {
            panel(graphics, currentLayout.contentX, currentLayout.contentTop, currentLayout.listW, currentLayout.contentBottom - currentLayout.contentTop);
            panel(graphics, currentLayout.detailX, currentLayout.contentTop, currentLayout.detailW, currentLayout.contentBottom - currentLayout.contentTop);
            panel(graphics, currentLayout.contentX, currentLayout.footerY, currentLayout.contentW, currentLayout.bottom - currentLayout.footerY);

            graphics.drawString(this.font, t("section.saved_accounts"), currentLayout.contentX + 10, currentLayout.contentTop + 9, TEXT);
            drawRightAligned(graphics, t("label.total", accounts.size()), currentLayout.contentX + currentLayout.listW - 10, currentLayout.contentTop + 9, MUTED);
            drawAccounts(graphics, mouseX, mouseY, currentLayout.contentBottom);
            drawDetails(graphics, currentLayout.detailX, currentLayout.contentTop, currentLayout.detailW, currentLayout.actionY);
        }

        int statusColor = busy ? WARNING : (statusError ? DANGER : SUCCESS);
        graphics.drawString(this.font, fit(status.getString(), currentLayout.contentW - 20), currentLayout.contentX + 10, currentLayout.statusY, statusColor);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderCompact(GuiGraphics graphics, int mouseX, int mouseY, ScreenLayout currentLayout) {
        panel(graphics, currentLayout.contentX, currentLayout.contentTop, currentLayout.contentW, currentLayout.contentBottom - currentLayout.contentTop);
        panel(graphics, currentLayout.contentX, currentLayout.footerY, currentLayout.contentW, currentLayout.bottom - currentLayout.footerY);

        graphics.drawString(this.font, t("section.saved_accounts"), currentLayout.contentX + 10, currentLayout.contentTop + 8, TEXT);
        drawRightAligned(graphics, t("label.total", accounts.size()), currentLayout.contentX + currentLayout.contentW - 10, currentLayout.contentTop + 8, MUTED);
        drawCompactSelection(graphics, currentLayout);
        drawAccounts(graphics, mouseX, mouseY, currentLayout.contentBottom);
    }

    private void drawCompactSelection(GuiGraphics graphics, ScreenLayout currentLayout) {
        MinecraftAccount account = selectedAccount();
        int x = currentLayout.contentX + 10;
        int w = currentLayout.contentW - 20;
        int y = currentLayout.contentTop + 26;

        if (account == null) {
            graphics.drawString(this.font, t("empty.select_account"), x, y, MUTED);
            graphics.drawString(this.font, t("empty.add_account"), x, y + 12, MUTED);
            return;
        }

        boolean current = account.getUsername().equals(currentUsername());
        String selectedLine = t("section.selected_account").getString() + ": " + account.getUsername();
        String metaLine = account.getKind().name() + "  |  " + t("label.expires").getString() + ": " + expiryText(account);
        graphics.drawString(this.font, fit(selectedLine, w), x, y, current ? SUCCESS : TEXT);
        graphics.drawString(this.font, fit(metaLine, w), x, y + 12, MUTED);
    }

    private void drawAccounts(GuiGraphics graphics, int mouseX, int mouseY, int contentBottom) {
        if (accounts.isEmpty()) {
            graphics.drawString(this.font, t("empty.no_accounts"), listX + 10, listY + 12, MUTED);
            return;
        }

        int y = listY;
        String currentName = currentUsername();
        for (MinecraftAccount account : accounts) {
            if (y + rowH > contentBottom - 6) {
                graphics.drawString(this.font, "...", listX + 12, y + 10, MUTED);
                break;
            }

            boolean selected = account.getUuid().equals(selectedUuid);
            boolean current = account.getUsername().equals(currentName);
            boolean hover = mouseX >= listX + 6 && mouseX <= listX + listW - 6 && mouseY >= y && mouseY <= y + rowH - 3;
            int rowColor = selected ? 0xFF243B5C : (hover ? 0xFF202838 : PANEL_DARK);
            graphics.fill(listX + 6, y, listX + listW - 6, y + rowH - 3, rowColor);
            if (selected) {
                graphics.fill(listX + 6, y, listX + 9, y + rowH - 3, ACCENT);
            }

            graphics.drawString(this.font, fit(account.getUsername(), listW - 84), listX + 15, y + 5, TEXT);
            graphics.drawString(this.font, current ? t("badge.current") : Component.literal(account.getKind().name()), listX + 15, y + 18, current ? SUCCESS : MUTED);
            y += rowH;
        }
    }

    private void drawDetails(GuiGraphics graphics, int x, int top, int w, int actionTop) {
        graphics.drawString(this.font, t("section.selected_account"), x + 10, top + 9, TEXT);
        MinecraftAccount account = selectedAccount();
        if (account == null) {
            graphics.drawString(this.font, t("empty.select_account"), x + 10, top + 38, MUTED);
            graphics.drawString(this.font, t("empty.add_account"), x + 10, top + 52, MUTED);
            return;
        }

        int y = top + 38;
        drawField(graphics, x + 10, y, t("label.name"), account.getUsername(), w - 20);
        y += 28;
        drawField(graphics, x + 10, y, t("label.uuid"), account.getUuid(), w - 20);
        y += 28;
        drawField(graphics, x + 10, y, t("label.source"), account.getKind().name(), w - 20);
        y += 28;
        drawField(graphics, x + 10, y, t("label.expires"), expiryText(account), w - 20);
        y += 34;

        boolean current = account.getUsername().equals(currentUsername());
        graphics.drawString(this.font, fit(t(current ? "status.active_session" : "status.ready_to_use").getString(), w - 20), x + 10, y, current ? SUCCESS : MUTED);
        graphics.drawString(this.font, fit(t("label.action_hint").getString(), w - 20), x + 10, actionTop - 16, MUTED);
    }

    private void drawField(GuiGraphics graphics, int x, int y, Component label, String value, int width) {
        graphics.drawString(this.font, label, x, y, MUTED);
        graphics.drawString(this.font, fit(value == null ? "-" : value, width), x, y + 11, TEXT);
    }

    private void drawRightAligned(GuiGraphics graphics, Component text, int right, int y, int color) {
        graphics.drawString(this.font, text, right - this.font.width(text.getString()), y, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int y = listY;
            for (MinecraftAccount account : accounts) {
                if (y + rowH > listBottom - 6) {
                    break;
                }
                if (mouseX >= listX + 6 && mouseX <= listX + listW - 6 && mouseY >= y && mouseY <= y + rowH - 3) {
                    selectedUuid = account.getUuid();
                    refreshScreen(t("status.ready"), false, selectedUuid);
                    return true;
                }
                y += rowH;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        returnToParent();
    }

    private void returnToParent() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    private void loginAccessToken() {
        if (busy) {
            return;
        }
        String token = AccessTokenSanitizer.extract(tokenField.getValue());
        if (token.isEmpty()) {
            setStatus(t("status.paste_token"), true);
            return;
        }
        tokenField.setValue(token);
        runBackground(t("status.importing_token"), () -> service.loginAccessToken(token));
    }

    private void loginMicrosoft() {
        if (busy) {
            return;
        }
        if (!AltManagerForgeClient.config().hasMicrosoftClientId()) {
            setStatus(t("status.missing_client_id"), true);
            return;
        }
        runBackground(t("status.waiting_microsoft"), () -> service.loginMicrosoft(session -> {
            showDeviceCode(session);
            Util.getPlatform().openUri(session.getVerificationUri());
        }));
    }

    private void showDeviceCode(DeviceCodeSession session) {
        Minecraft.getInstance().execute(() -> {
            String uri = session.getVerificationUriComplete() == null
                    ? session.getVerificationUri()
                    : session.getVerificationUriComplete();
            setStatus(t("status.code_copied", session.getUserCode(), uri), false);
            Minecraft.getInstance().keyboardHandler.setClipboard(session.getUserCode());
        });
    }

    private void refreshSelected() {
        MinecraftAccount account = selectedAccount();
        if (account == null) {
            setStatus(t("status.select_microsoft"), true);
            return;
        }
        if (account.getKind() != AccountKind.MICROSOFT) {
            setStatus(t("status.refresh_microsoft_only"), true);
            return;
        }
        runBackground(t("status.refreshing", account.getUsername()), () -> service.refresh(account));
    }

    private void deleteSelected() {
        MinecraftAccount account = selectedAccount();
        if (account == null) {
            setStatus(t("status.select_delete"), true);
            return;
        }
        try {
            service.remove(account.getUuid());
            refreshScreen(t("status.deleted", account.getUsername()), false, null);
        } catch (IOException exception) {
            setStatus(t("status.delete_failed", exception.getMessage()), true);
        }
    }

    private void switchSelected() {
        MinecraftAccount account = selectedAccount();
        if (account == null) {
            setStatus(t("status.select_use"), true);
            return;
        }
        SessionSwitchResult result = service.switchTo(Minecraft.getInstance(), account, AltManagerForgeClient.sessionAdapter());
        if (result.isSuccess()) {
            refreshScreen(t("status.switched", account.getUsername()), false, account.getUuid());
        } else {
            setStatus(t("status.failed", result.getMessage()), true);
        }
    }

    private void runBackground(Component startMessage, AccountTask task) {
        busy = true;
        setStatus(startMessage, false);
        Thread thread = new Thread(() -> {
            try {
                MinecraftAccount account = task.run();
                Minecraft.getInstance().execute(() ->
                        refreshScreen(t("status.saved", account.getUsername()), false, account.getUuid()));
            } catch (Exception exception) {
                Minecraft.getInstance().execute(() -> {
                    busy = false;
                    setStatus(t("status.failed", exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage()), true);
                });
            }
        }, "AltManager-Login");
        thread.setDaemon(true);
        thread.start();
    }

    private void reloadAccounts() {
        try {
            accounts = service.listAccounts();
        } catch (IOException exception) {
            accounts = new ArrayList<>();
            setStatus(t("status.read_failed", exception.getMessage()), true);
        }
    }

    private void ensureSelection() {
        if (accounts.isEmpty()) {
            selectedUuid = null;
            return;
        }
        if (findAccount(selectedUuid) != null) {
            return;
        }
        String currentName = currentUsername();
        for (MinecraftAccount account : accounts) {
            if (account.getUsername().equals(currentName)) {
                selectedUuid = account.getUuid();
                return;
            }
        }
        selectedUuid = accounts.get(0).getUuid();
    }

    private MinecraftAccount selectedAccount() {
        return findAccount(selectedUuid);
    }

    private MinecraftAccount findAccount(String uuid) {
        if (uuid == null) {
            return null;
        }
        for (MinecraftAccount account : accounts) {
            if (uuid.equals(account.getUuid())) {
                return account;
            }
        }
        return null;
    }

    private void refreshScreen(Component nextStatus, boolean error, String nextSelectedUuid) {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new AltManagerScreen(parent, nextStatus, error, nextSelectedUuid));
        }
    }

    private void setStatus(Component status, boolean error) {
        this.status = status == null ? t("status.ready") : status;
        this.statusError = error;
    }

    private String currentUsername() {
        Minecraft client = Minecraft.getInstance();
        return client == null || client.getUser() == null ? "" : client.getUser().getName();
    }

    private String expiryText(MinecraftAccount account) {
        Instant expiresAt = account.getAccessTokenExpiresAt();
        return expiresAt == null ? t("label.unknown").getString() : expiresAt.toString();
    }

    private ScreenLayout createLayout() {
        ScreenLayout next = new ScreenLayout();
        int margin = this.width < 520 ? 8 : 14;
        next.compact = this.width < 900 || this.height < 560;
        next.contentTop = this.height < 420 ? 30 : 34;
        next.bottom = Math.max(next.contentTop + 128, this.height - margin);
        next.contentW = next.compact
                ? Math.max(1, this.width - margin * 2)
                : Math.min(860, Math.max(1, this.width - margin * 2));
        next.contentX = (this.width - next.contentW) / 2;

        int availableH = Math.max(120, next.bottom - next.contentTop);
        int footerH = next.compact ? clamp(availableH / 3, 90, 106) : 66;
        next.footerY = next.bottom - footerH;
        next.contentBottom = Math.max(next.contentTop + 72, next.footerY - 8);
        if (next.contentBottom > next.footerY - 4) {
            next.contentBottom = next.footerY - 4;
        }

        next.tokenY = next.compact ? next.footerY + 22 : next.footerY + 6;
        next.statusY = next.compact ? next.footerY + 7 : next.footerY + 31;
        next.actionY = next.compact ? next.footerY + 46 : next.contentBottom - 70;
        next.navY = next.compact ? next.footerY + 70 : next.bottom - 24;

        if (next.compact) {
            next.listX = next.contentX;
            next.listY = next.contentTop + 54;
            next.listW = next.contentW;
            next.listBottom = next.contentBottom;
            next.rowH = 30;
        } else {
            int gap = 10;
            next.listX = next.contentX;
            next.listY = next.contentTop + 28;
            next.listW = clamp((next.contentW * 42) / 100, 280, 360);
            next.listBottom = next.contentBottom;
            next.rowH = 32;
            next.detailX = next.contentX + next.listW + gap;
            next.detailW = next.contentW - next.listW - gap;
        }
        return next;
    }

    private String fit(String value, int width) {
        if (value == null || width <= 0) {
            return "";
        }
        if (this.font.width(value) <= width) {
            return value;
        }
        String ellipsis = "...";
        int max = Math.max(1, value.length());
        while (max > 0) {
            String candidate = value.substring(0, max) + ellipsis;
            if (this.font.width(candidate) <= width) {
                return candidate;
            }
            max--;
        }
        return ellipsis;
    }

    private void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, PANEL);
        graphics.fill(x, y, x + width, y + 1, BORDER);
        graphics.fill(x, y + height - 1, x + width, y + height, BORDER);
        graphics.fill(x, y, x + 1, y + height, BORDER);
        graphics.fill(x + width - 1, y, x + width, y + height, BORDER);
    }

    private Button button(Component label, int x, int y, int width, int height, Button.OnPress press) {
        return Button.builder(label, press).bounds(x, y, width, height).build();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Component t(String key, Object... args) {
        return Component.translatable("altmanager." + key, args);
    }

    private static final class ScreenLayout {
        private boolean compact;
        private int contentX;
        private int contentW;
        private int contentTop;
        private int contentBottom;
        private int footerY;
        private int bottom;
        private int listX;
        private int listY;
        private int listW;
        private int listBottom;
        private int rowH;
        private int detailX;
        private int detailW;
        private int statusY;
        private int tokenY;
        private int actionY;
        private int navY;
    }

    private interface AccountTask {
        MinecraftAccount run() throws Exception;
    }
}
