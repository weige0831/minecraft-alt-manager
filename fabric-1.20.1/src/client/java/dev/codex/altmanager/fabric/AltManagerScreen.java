package dev.codex.altmanager.fabric;

import dev.codex.altmanager.AltManagerService;
import dev.codex.altmanager.AccountKind;
import dev.codex.altmanager.MinecraftAccount;
import dev.codex.altmanager.auth.AccessTokenSanitizer;
import dev.codex.altmanager.auth.DeviceCodeSession;
import dev.codex.altmanager.session.SessionSwitchResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

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
    private Text status;
    private boolean statusError;
    private String selectedUuid;
    private List<MinecraftAccount> accounts = new ArrayList<MinecraftAccount>();
    private TextFieldWidget tokenField;
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

    private AltManagerScreen(Screen parent, Text status, boolean statusError, String selectedUuid) {
        super(t("screen.title"));
        this.parent = parent;
        this.status = status == null ? t("status.ready") : status;
        this.statusError = statusError;
        this.selectedUuid = selectedUuid;
        this.service = AltManagerFabricClient.service();
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

        tokenField = new TextFieldWidget(this.textRenderer, layout.contentX, layout.tokenY, layout.contentW, 20, t("field.access_token"));
        tokenField.setMaxLength(8192);
        tokenField.setPlaceholder(t("placeholder.access_token"));
        this.addDrawableChild(tokenField);

        if (layout.compact) {
            initCompactButtons(selected);
            return;
        }

        ButtonWidget useButton = ButtonWidget.builder(t("button.use_selected"), button -> switchSelected())
                .dimensions(layout.detailX, layout.actionY, layout.detailW, 20)
                .build();
        useButton.active = selected != null && !busy;
        this.addDrawableChild(useButton);

        ButtonWidget refreshButton = ButtonWidget.builder(t("button.refresh"), button -> refreshSelected())
                .dimensions(layout.detailX, layout.actionY + 24, (layout.detailW - 6) / 2, 20)
                .build();
        refreshButton.active = selected != null && selected.getKind() == AccountKind.MICROSOFT && !busy;
        this.addDrawableChild(refreshButton);

        ButtonWidget deleteButton = ButtonWidget.builder(t("button.delete"), button -> deleteSelected())
                .dimensions(layout.detailX + (layout.detailW + 6) / 2, layout.actionY + 24, (layout.detailW - 6) / 2, 20)
                .build();
        deleteButton.active = selected != null && !busy;
        this.addDrawableChild(deleteButton);

        this.addDrawableChild(ButtonWidget.builder(t("button.back"), button -> close())
                .dimensions(layout.contentX, layout.navY, 128, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(t("button.reload"), button -> refreshScreen(t("status.reloaded"), false, selectedUuid))
                .dimensions(layout.contentX + 134, layout.navY, 118, 20)
                .build());

        ButtonWidget microsoftButton = ButtonWidget.builder(t("button.microsoft"), button -> loginMicrosoft())
                .dimensions(layout.contentX + layout.contentW - 282, layout.navY, 132, 20)
                .build();
        microsoftButton.active = !busy;
        this.addDrawableChild(microsoftButton);

        ButtonWidget importButton = ButtonWidget.builder(t("button.import_token"), button -> loginAccessToken())
                .dimensions(layout.contentX + layout.contentW - 144, layout.navY, 144, 20)
                .build();
        importButton.active = !busy;
        this.addDrawableChild(importButton);
    }

    private void initCompactButtons(MinecraftAccount selected) {
        int gap = 4;
        int useW = Math.max(92, (layout.contentW * 42) / 100);
        int sideW = Math.max(64, (layout.contentW - useW - gap * 2) / 2);
        if (useW + sideW * 2 + gap * 2 > layout.contentW) {
            sideW = Math.max(54, (layout.contentW - useW - gap * 2) / 2);
        }

        ButtonWidget useButton = ButtonWidget.builder(t("button.use_short"), button -> switchSelected())
                .dimensions(layout.contentX, layout.actionY, useW, 20)
                .build();
        useButton.active = selected != null && !busy;
        this.addDrawableChild(useButton);

        ButtonWidget refreshButton = ButtonWidget.builder(t("button.refresh_short"), button -> refreshSelected())
                .dimensions(layout.contentX + useW + gap, layout.actionY, sideW, 20)
                .build();
        refreshButton.active = selected != null && selected.getKind() == AccountKind.MICROSOFT && !busy;
        this.addDrawableChild(refreshButton);

        ButtonWidget deleteButton = ButtonWidget.builder(t("button.delete"), button -> deleteSelected())
                .dimensions(layout.contentX + useW + sideW + gap * 2, layout.actionY, layout.contentW - useW - sideW - gap * 2, 20)
                .build();
        deleteButton.active = selected != null && !busy;
        this.addDrawableChild(deleteButton);

        int navW = (layout.contentW - gap * 3) / 4;
        this.addDrawableChild(ButtonWidget.builder(t("button.back"), button -> close())
                .dimensions(layout.contentX, layout.navY, navW, 20)
                .build());
        this.addDrawableChild(ButtonWidget.builder(t("button.reload"), button -> refreshScreen(t("status.reloaded"), false, selectedUuid))
                .dimensions(layout.contentX + navW + gap, layout.navY, navW, 20)
                .build());

        ButtonWidget microsoftButton = ButtonWidget.builder(t("button.microsoft_short"), button -> loginMicrosoft())
                .dimensions(layout.contentX + (navW + gap) * 2, layout.navY, navW, 20)
                .build();
        microsoftButton.active = !busy;
        this.addDrawableChild(microsoftButton);

        ButtonWidget importButton = ButtonWidget.builder(t("button.import_short"), button -> loginAccessToken())
                .dimensions(layout.contentX + (navW + gap) * 3, layout.navY, layout.contentW - (navW + gap) * 3, 20)
                .build();
        importButton.active = !busy;
        this.addDrawableChild(importButton);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        ScreenLayout layout = this.layout == null ? createLayout() : this.layout;
        context.fill(0, 0, this.width, this.height, 0xF0101117);
        context.fill(0, 0, this.width, 26, 0xFF151A24);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 9, TEXT);

        if (layout.compact) {
            renderCompact(context, mouseX, mouseY, layout);
        } else {
            panel(context, layout.contentX, layout.contentTop, layout.listW, layout.contentBottom - layout.contentTop);
            panel(context, layout.detailX, layout.contentTop, layout.detailW, layout.contentBottom - layout.contentTop);
            panel(context, layout.contentX, layout.footerY, layout.contentW, layout.bottom - layout.footerY);

            context.drawTextWithShadow(this.textRenderer, t("section.saved_accounts"), layout.contentX + 10, layout.contentTop + 9, TEXT);
            drawRightAligned(context, t("label.total", accounts.size()), layout.contentX + layout.listW - 10, layout.contentTop + 9, MUTED);
            drawAccounts(context, mouseX, mouseY, layout.contentBottom);

            drawDetails(context, layout.detailX, layout.contentTop, layout.detailW, layout.actionY);
        }

        int statusColor = busy ? WARNING : (statusError ? DANGER : SUCCESS);
        context.drawTextWithShadow(this.textRenderer, fit(status.getString(), layout.contentW - 20), layout.contentX + 10, layout.statusY, statusColor);
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderCompact(DrawContext context, int mouseX, int mouseY, ScreenLayout layout) {
        panel(context, layout.contentX, layout.contentTop, layout.contentW, layout.contentBottom - layout.contentTop);
        panel(context, layout.contentX, layout.footerY, layout.contentW, layout.bottom - layout.footerY);

        context.drawTextWithShadow(this.textRenderer, t("section.saved_accounts"), layout.contentX + 10, layout.contentTop + 8, TEXT);
        drawRightAligned(context, t("label.total", accounts.size()), layout.contentX + layout.contentW - 10, layout.contentTop + 8, MUTED);
        drawCompactSelection(context, layout);
        drawAccounts(context, mouseX, mouseY, layout.contentBottom);
    }

    private void drawCompactSelection(DrawContext context, ScreenLayout layout) {
        MinecraftAccount account = selectedAccount();
        int x = layout.contentX + 10;
        int w = layout.contentW - 20;
        int y = layout.contentTop + 26;

        if (account == null) {
            context.drawTextWithShadow(this.textRenderer, t("empty.select_account"), x, y, MUTED);
            context.drawTextWithShadow(this.textRenderer, t("empty.add_account"), x, y + 12, MUTED);
            return;
        }

        boolean current = account.getUsername().equals(currentUsername());
        String selectedLine = t("section.selected_account").getString() + ": " + account.getUsername();
        String metaLine = account.getKind().name() + "  |  " + t("label.expires").getString() + ": " + expiryText(account);
        context.drawTextWithShadow(this.textRenderer, fit(selectedLine, w), x, y, current ? SUCCESS : TEXT);
        context.drawTextWithShadow(this.textRenderer, fit(metaLine, w), x, y + 12, MUTED);
    }

    private void drawAccounts(DrawContext context, int mouseX, int mouseY, int contentBottom) {
        if (accounts.isEmpty()) {
            context.drawTextWithShadow(this.textRenderer, t("empty.no_accounts"), listX + 10, listY + 12, MUTED);
            return;
        }

        int y = listY;
        String currentName = currentUsername();
        for (MinecraftAccount account : accounts) {
            if (y + rowH > contentBottom - 6) {
                context.drawTextWithShadow(this.textRenderer, "...", listX + 12, y + 10, MUTED);
                break;
            }

            boolean selected = account.getUuid().equals(selectedUuid);
            boolean current = account.getUsername().equals(currentName);
            boolean hover = mouseX >= listX + 6 && mouseX <= listX + listW - 6 && mouseY >= y && mouseY <= y + rowH - 3;
            int rowColor = selected ? 0xFF243B5C : (hover ? 0xFF202838 : PANEL_DARK);
            context.fill(listX + 6, y, listX + listW - 6, y + rowH - 3, rowColor);
            if (selected) {
                context.fill(listX + 6, y, listX + 9, y + rowH - 3, ACCENT);
            }

            context.drawTextWithShadow(this.textRenderer, fit(account.getUsername(), listW - 84), listX + 15, y + 5, TEXT);
            context.drawTextWithShadow(this.textRenderer, current ? t("badge.current") : Text.literal(account.getKind().name()), listX + 15, y + 18, current ? SUCCESS : MUTED);
            y += rowH;
        }
    }

    private void drawDetails(DrawContext context, int x, int top, int w, int actionTop) {
        context.drawTextWithShadow(this.textRenderer, t("section.selected_account"), x + 10, top + 9, TEXT);
        MinecraftAccount account = selectedAccount();
        if (account == null) {
            context.drawTextWithShadow(this.textRenderer, t("empty.select_account"), x + 10, top + 38, MUTED);
            context.drawTextWithShadow(this.textRenderer, t("empty.add_account"), x + 10, top + 52, MUTED);
            return;
        }

        int y = top + 38;
        drawField(context, x + 10, y, t("label.name"), account.getUsername(), w - 20);
        y += 28;
        drawField(context, x + 10, y, t("label.uuid"), account.getUuid(), w - 20);
        y += 28;
        drawField(context, x + 10, y, t("label.source"), account.getKind().name(), w - 20);
        y += 28;
        drawField(context, x + 10, y, t("label.expires"), expiryText(account), w - 20);
        y += 34;

        boolean current = account.getUsername().equals(currentUsername());
        context.drawTextWithShadow(this.textRenderer, fit(t(current ? "status.active_session" : "status.ready_to_use").getString(), w - 20), x + 10, y, current ? SUCCESS : MUTED);
        context.drawTextWithShadow(this.textRenderer, fit(t("label.action_hint").getString(), w - 20), x + 10, actionTop - 16, MUTED);
    }

    private void drawField(DrawContext context, int x, int y, Text label, String value, int width) {
        context.drawTextWithShadow(this.textRenderer, label, x, y, MUTED);
        context.drawTextWithShadow(this.textRenderer, fit(value == null ? "-" : value, width), x, y + 11, TEXT);
    }

    private void drawRightAligned(DrawContext context, Text text, int right, int y, int color) {
        context.drawTextWithShadow(this.textRenderer, text, right - this.textRenderer.getWidth(text.getString()), y, color);
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
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    private void loginAccessToken() {
        if (busy) {
            return;
        }
        String token = AccessTokenSanitizer.extract(tokenField.getText());
        if (token.isEmpty()) {
            setStatus(t("status.paste_token"), true);
            return;
        }
        tokenField.setText(token);
        runBackground(t("status.importing_token"), () -> service.loginAccessToken(token));
    }

    private void loginMicrosoft() {
        if (busy) {
            return;
        }
        if (!AltManagerFabricClient.config().hasMicrosoftClientId()) {
            setStatus(t("status.missing_client_id"), true);
            return;
        }
        runBackground(t("status.waiting_microsoft"), () -> service.loginMicrosoft(session -> {
            showDeviceCode(session);
            Util.getOperatingSystem().open(session.getVerificationUri());
        }));
    }

    private void showDeviceCode(DeviceCodeSession session) {
        MinecraftClient.getInstance().execute(() -> {
            String uri = session.getVerificationUriComplete() == null
                    ? session.getVerificationUri()
                    : session.getVerificationUriComplete();
            setStatus(t("status.code_copied", session.getUserCode(), uri), false);
            MinecraftClient.getInstance().keyboard.setClipboard(session.getUserCode());
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
        SessionSwitchResult result = service.switchTo(MinecraftClient.getInstance(), account, AltManagerFabricClient.sessionAdapter());
        if (result.isSuccess()) {
            refreshScreen(t("status.switched", account.getUsername()), false, account.getUuid());
        } else {
            setStatus(t("status.failed", result.getMessage()), true);
        }
    }

    private void runBackground(Text startMessage, AccountTask task) {
        busy = true;
        setStatus(startMessage, false);
        Thread thread = new Thread(() -> {
            try {
                MinecraftAccount account = task.run();
                MinecraftClient.getInstance().execute(() ->
                        refreshScreen(t("status.saved", account.getUsername()), false, account.getUuid()));
            } catch (Exception exception) {
                MinecraftClient.getInstance().execute(() -> {
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
            accounts = new ArrayList<MinecraftAccount>();
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

    private void refreshScreen(Text nextStatus, boolean error, String nextSelectedUuid) {
        if (this.client != null) {
            this.client.setScreen(new AltManagerScreen(parent, nextStatus, error, nextSelectedUuid));
        }
    }

    private void setStatus(Text status, boolean error) {
        this.status = status == null ? t("status.ready") : status;
        this.statusError = error;
    }

    private String currentUsername() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client == null || client.getSession() == null ? "" : client.getSession().getUsername();
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
        if (value == null) {
            return "";
        }
        if (width <= 0) {
            return "";
        }
        if (this.textRenderer.getWidth(value) <= width) {
            return value;
        }
        String ellipsis = "...";
        int max = Math.max(1, value.length());
        while (max > 0) {
            String candidate = value.substring(0, max) + ellipsis;
            if (this.textRenderer.getWidth(candidate) <= width) {
                return candidate;
            }
            max--;
        }
        return ellipsis;
    }

    private void panel(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, PANEL);
        context.fill(x, y, x + width, y + 1, BORDER);
        context.fill(x, y + height - 1, x + width, y + height, BORDER);
        context.fill(x, y, x + 1, y + height, BORDER);
        context.fill(x + width - 1, y, x + width, y + height, BORDER);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Text t(String key, Object... args) {
        return Text.translatable("altmanager." + key, args);
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
