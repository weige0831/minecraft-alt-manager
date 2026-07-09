package dev.codex.altmanager.forge112;

import dev.codex.altmanager.AccountKind;
import dev.codex.altmanager.AltManagerService;
import dev.codex.altmanager.MinecraftAccount;
import dev.codex.altmanager.auth.AccessTokenSanitizer;
import dev.codex.altmanager.auth.DeviceCodeSession;
import dev.codex.altmanager.session.SessionSwitchResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class AltManagerGui112 extends GuiScreen {
    private static final int PANEL = 0xC8202430;
    private static final int PANEL_DARK = 0xDD11151F;
    private static final int BORDER = 0xFF3B4254;
    private static final int ACCENT = 0xFF6AA9FF;
    private static final int SUCCESS = 0xFFA7E3A1;
    private static final int WARNING = 0xFFFFD27F;
    private static final int TEXT = 0xFFE8EDF7;
    private static final int MUTED = 0xFF9AA6BA;
    private static final int DANGER = 0xFFFF8E8E;
    private static final int MAX_BUTTON_WIDTH = 200;

    private static final int BTN_USE = 1;
    private static final int BTN_REFRESH = 2;
    private static final int BTN_DELETE = 3;
    private static final int BTN_BACK = 4;
    private static final int BTN_RELOAD = 5;
    private static final int BTN_MICROSOFT = 6;
    private static final int BTN_IMPORT = 7;

    private final GuiScreen parent;
    private final AltManagerService service;
    private String status = tr("altmanager.status.ready");
    private boolean statusError;
    private String selectedUuid;
    private List<MinecraftAccount> accounts = new ArrayList<MinecraftAccount>();
    private GuiTextField tokenField;
    private boolean busy;
    private int listX;
    private int listY;
    private int listW;
    private int listBottom;
    private int rowH;
    private Layout layout;

    public AltManagerGui112(GuiScreen parent) {
        this(parent, tr("altmanager.status.ready"), false, null);
    }

    private AltManagerGui112(GuiScreen parent, String status, boolean statusError, String selectedUuid) {
        this.parent = parent;
        this.status = status == null ? tr("altmanager.status.ready") : status;
        this.statusError = statusError;
        this.selectedUuid = selectedUuid;
        this.service = AltManagerForge112.service();
    }

    @Override
    public void initGui() {
        reloadAccounts();
        ensureSelection();
        this.layout = createLayout();
        this.listX = layout.listX;
        this.listY = layout.listY;
        this.listW = layout.listW;
        this.listBottom = layout.listBottom;
        this.rowH = layout.rowH;

        this.buttonList.clear();
        MinecraftAccount selected = selectedAccount();
        tokenField = new GuiTextField(100, this.fontRenderer, layout.contentX, layout.tokenY, layout.contentW, 20);
        tokenField.setMaxStringLength(8192);

        if (layout.compact) {
            initCompactButtons(selected);
            return;
        }

        int actionW = Math.min(MAX_BUTTON_WIDTH, layout.detailW);
        int actionX = layout.detailX + Math.max(0, (layout.detailW - actionW) / 2);
        int halfW = (actionW - 6) / 2;

        GuiButton useButton = button(BTN_USE, actionX, layout.actionY, actionW, tr("altmanager.button.use_selected"));
        useButton.enabled = selected != null && !busy;
        this.buttonList.add(useButton);

        GuiButton refreshButton = button(BTN_REFRESH, actionX, layout.actionY + 24, halfW, tr("altmanager.button.refresh"));
        refreshButton.enabled = selected != null && selected.getKind() == AccountKind.MICROSOFT && !busy;
        this.buttonList.add(refreshButton);

        GuiButton deleteButton = button(BTN_DELETE, actionX + halfW + 6, layout.actionY + 24, actionW - halfW - 6, tr("altmanager.button.delete"));
        deleteButton.enabled = selected != null && !busy;
        this.buttonList.add(deleteButton);

        this.buttonList.add(button(BTN_BACK, layout.contentX, layout.navY, 128, tr("altmanager.button.back")));
        this.buttonList.add(button(BTN_RELOAD, layout.contentX + 134, layout.navY, 118, tr("altmanager.button.reload")));

        GuiButton microsoftButton = button(BTN_MICROSOFT, layout.contentX + layout.contentW - 282, layout.navY, 132, tr("altmanager.button.microsoft"));
        microsoftButton.enabled = !busy;
        this.buttonList.add(microsoftButton);

        GuiButton importButton = button(BTN_IMPORT, layout.contentX + layout.contentW - 144, layout.navY, 144, tr("altmanager.button.import_token"));
        importButton.enabled = !busy;
        this.buttonList.add(importButton);
    }

    private void initCompactButtons(MinecraftAccount selected) {
        int gap = 4;
        int useW = Math.max(92, (layout.contentW * 42) / 100);
        int sideW = Math.max(64, (layout.contentW - useW - gap * 2) / 2);
        if (useW + sideW * 2 + gap * 2 > layout.contentW) {
            sideW = Math.max(54, (layout.contentW - useW - gap * 2) / 2);
        }
        GuiButton useButton = button(BTN_USE, layout.contentX, layout.actionY, useW, tr("altmanager.button.use_short"));
        useButton.enabled = selected != null && !busy;
        this.buttonList.add(useButton);
        GuiButton refreshButton = button(BTN_REFRESH, layout.contentX + useW + gap, layout.actionY, sideW, tr("altmanager.button.refresh_short"));
        refreshButton.enabled = selected != null && selected.getKind() == AccountKind.MICROSOFT && !busy;
        this.buttonList.add(refreshButton);
        GuiButton deleteButton = button(BTN_DELETE, layout.contentX + useW + sideW + gap * 2, layout.actionY, layout.contentW - useW - sideW - gap * 2, tr("altmanager.button.delete"));
        deleteButton.enabled = selected != null && !busy;
        this.buttonList.add(deleteButton);

        int navW = (layout.contentW - gap * 3) / 4;
        this.buttonList.add(button(BTN_BACK, layout.contentX, layout.navY, navW, tr("altmanager.button.back")));
        this.buttonList.add(button(BTN_RELOAD, layout.contentX + navW + gap, layout.navY, navW, tr("altmanager.button.reload")));
        GuiButton microsoftButton = button(BTN_MICROSOFT, layout.contentX + (navW + gap) * 2, layout.navY, navW, tr("altmanager.button.microsoft_short"));
        microsoftButton.enabled = !busy;
        this.buttonList.add(microsoftButton);
        GuiButton importButton = button(BTN_IMPORT, layout.contentX + (navW + gap) * 3, layout.navY, layout.contentW - (navW + gap) * 3, tr("altmanager.button.import_short"));
        importButton.enabled = !busy;
        this.buttonList.add(importButton);
    }

    private GuiButton button(int id, int x, int y, int width, String label) {
        return new GuiButton(id, x, y, Math.min(MAX_BUTTON_WIDTH, Math.max(36, width)), 20, label);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        Layout current = this.layout == null ? createLayout() : this.layout;
        drawRect(0, 0, this.width, this.height, 0xF0101117);
        drawRect(0, 0, this.width, 26, 0xFF151A24);
        drawCenteredString(this.fontRenderer, tr("altmanager.screen.title"), this.width / 2, 9, TEXT);

        if (current.compact) {
            renderCompact(mouseX, mouseY, current);
        } else {
            panel(current.contentX, current.contentTop, current.listW, current.contentBottom - current.contentTop);
            panel(current.detailX, current.contentTop, current.detailW, current.contentBottom - current.contentTop);
            panel(current.contentX, current.footerY, current.contentW, current.bottom - current.footerY);
            drawString(this.fontRenderer, tr("altmanager.section.saved_accounts"), current.contentX + 10, current.contentTop + 9, TEXT);
            drawRightAligned(tr("altmanager.label.total", accounts.size()), current.contentX + current.listW - 10, current.contentTop + 9, MUTED);
            drawAccounts(mouseX, mouseY, current.contentBottom);
            drawDetails(current.detailX, current.contentTop, current.detailW, current.actionY);
        }

        int statusColor = busy ? WARNING : (statusError ? DANGER : SUCCESS);
        drawString(this.fontRenderer, fit(status, current.contentW - 20), current.contentX + 10, current.statusY, statusColor);
        tokenField.drawTextBox();
        if (tokenField.getText().isEmpty() && !tokenField.isFocused()) {
            drawString(this.fontRenderer, tr("altmanager.placeholder.access_token"), current.contentX + 4, current.tokenY + 6, MUTED);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void renderCompact(int mouseX, int mouseY, Layout current) {
        panel(current.contentX, current.contentTop, current.contentW, current.contentBottom - current.contentTop);
        panel(current.contentX, current.footerY, current.contentW, current.bottom - current.footerY);
        drawString(this.fontRenderer, tr("altmanager.section.saved_accounts"), current.contentX + 10, current.contentTop + 8, TEXT);
        drawRightAligned(tr("altmanager.label.total", accounts.size()), current.contentX + current.contentW - 10, current.contentTop + 8, MUTED);
        drawCompactSelection(current);
        drawAccounts(mouseX, mouseY, current.contentBottom);
    }

    private void drawCompactSelection(Layout current) {
        MinecraftAccount account = selectedAccount();
        int x = current.contentX + 10;
        int w = current.contentW - 20;
        int y = current.contentTop + 26;
        if (account == null) {
            drawString(this.fontRenderer, tr("altmanager.empty.select_account"), x, y, MUTED);
            drawString(this.fontRenderer, tr("altmanager.empty.add_account"), x, y + 12, MUTED);
            return;
        }
        boolean isCurrent = account.getUsername().equals(currentUsername());
        drawString(this.fontRenderer, fit(tr("altmanager.section.selected_account") + ": " + account.getUsername(), w), x, y, isCurrent ? SUCCESS : TEXT);
        drawString(this.fontRenderer, fit(account.getKind().name() + "  |  " + tr("altmanager.label.expires") + ": " + expiryText(account), w), x, y + 12, MUTED);
    }

    private void drawAccounts(int mouseX, int mouseY, int contentBottom) {
        if (accounts.isEmpty()) {
            drawString(this.fontRenderer, tr("altmanager.empty.no_accounts"), listX + 10, listY + 12, MUTED);
            return;
        }
        int y = listY;
        String currentName = currentUsername();
        for (MinecraftAccount account : accounts) {
            if (y + rowH > contentBottom - 6) {
                drawString(this.fontRenderer, "...", listX + 12, y + 10, MUTED);
                break;
            }
            boolean selected = account.getUuid().equals(selectedUuid);
            boolean isCurrent = account.getUsername().equals(currentName);
            boolean hover = mouseX >= listX + 6 && mouseX <= listX + listW - 6 && mouseY >= y && mouseY <= y + rowH - 3;
            drawRect(listX + 6, y, listX + listW - 6, y + rowH - 3, selected ? 0xFF243B5C : (hover ? 0xFF202838 : PANEL_DARK));
            if (selected) {
                drawRect(listX + 6, y, listX + 9, y + rowH - 3, ACCENT);
            }
            drawString(this.fontRenderer, fit(account.getUsername(), listW - 84), listX + 15, y + 5, TEXT);
            drawString(this.fontRenderer, isCurrent ? tr("altmanager.badge.current") : account.getKind().name(), listX + 15, y + 18, isCurrent ? SUCCESS : MUTED);
            y += rowH;
        }
    }

    private void drawDetails(int x, int top, int w, int actionTop) {
        drawString(this.fontRenderer, tr("altmanager.section.selected_account"), x + 10, top + 9, TEXT);
        MinecraftAccount account = selectedAccount();
        if (account == null) {
            drawString(this.fontRenderer, tr("altmanager.empty.select_account"), x + 10, top + 38, MUTED);
            drawString(this.fontRenderer, tr("altmanager.empty.add_account"), x + 10, top + 52, MUTED);
            return;
        }
        int y = top + 38;
        drawField(x + 10, y, tr("altmanager.label.name"), account.getUsername(), w - 20);
        y += 28;
        drawField(x + 10, y, tr("altmanager.label.uuid"), account.getUuid(), w - 20);
        y += 28;
        drawField(x + 10, y, tr("altmanager.label.source"), account.getKind().name(), w - 20);
        y += 28;
        drawField(x + 10, y, tr("altmanager.label.expires"), expiryText(account), w - 20);
        y += 34;
        boolean isCurrent = account.getUsername().equals(currentUsername());
        drawString(this.fontRenderer, fit(tr(isCurrent ? "altmanager.status.active_session" : "altmanager.status.ready_to_use"), w - 20), x + 10, y, isCurrent ? SUCCESS : MUTED);
        drawString(this.fontRenderer, fit(tr("altmanager.label.action_hint"), w - 20), x + 10, actionTop - 16, MUTED);
    }

    private void drawField(int x, int y, String label, String value, int width) {
        drawString(this.fontRenderer, label, x, y, MUTED);
        drawString(this.fontRenderer, fit(value == null ? "-" : value, width), x, y + 11, TEXT);
    }

    private void drawRightAligned(String text, int right, int y, int color) {
        drawString(this.fontRenderer, text, right - this.fontRenderer.getStringWidth(text), y, color);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        tokenField.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0) {
            int y = listY;
            for (MinecraftAccount account : accounts) {
                if (y + rowH > listBottom - 6) {
                    break;
                }
                if (mouseX >= listX + 6 && mouseX <= listX + listW - 6 && mouseY >= y && mouseY <= y + rowH - 3) {
                    selectedUuid = account.getUuid();
                    refreshScreen(tr("altmanager.status.ready"), false, selectedUuid);
                    return;
                }
                y += rowH;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (!tokenField.textboxKeyTyped(typedChar, keyCode)) {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BTN_USE) {
            switchSelected();
        } else if (button.id == BTN_REFRESH) {
            refreshSelected();
        } else if (button.id == BTN_DELETE) {
            deleteSelected();
        } else if (button.id == BTN_BACK) {
            this.mc.displayGuiScreen(parent);
        } else if (button.id == BTN_RELOAD) {
            refreshScreen(tr("altmanager.status.reloaded"), false, selectedUuid);
        } else if (button.id == BTN_MICROSOFT) {
            loginMicrosoft();
        } else if (button.id == BTN_IMPORT) {
            loginAccessToken();
        }
    }

    @Override
    public void onGuiClosed() {
        if (tokenField != null) {
            tokenField.setFocused(false);
        }
    }

    private void loginAccessToken() {
        if (busy) {
            return;
        }
        String token = AccessTokenSanitizer.extract(tokenField.getText());
        if (token.isEmpty()) {
            setStatus(tr("altmanager.status.paste_token"), true);
            return;
        }
        tokenField.setText(token);
        runBackground(tr("altmanager.status.importing_token"), new AccountTask() {
            @Override
            public MinecraftAccount run() throws Exception {
                return service.loginAccessToken(token);
            }
        });
    }

    private void loginMicrosoft() {
        if (busy) {
            return;
        }
        if (!AltManagerForge112.config().hasMicrosoftClientId()) {
            setStatus(tr("altmanager.status.missing_client_id"), true);
            return;
        }
        runBackground(tr("altmanager.status.waiting_microsoft"), new AccountTask() {
            @Override
            public MinecraftAccount run() throws Exception {
                return service.loginMicrosoft(session -> {
                    showDeviceCode(session);
                    openUri(session.getVerificationUri());
                });
            }
        });
    }

    private void showDeviceCode(final DeviceCodeSession session) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                setStatus(tr("altmanager.status.code_copied", session.getUserCode()), false);
                GuiScreen.setClipboardString(session.getUserCode());
            }
        });
    }

    private void refreshSelected() {
        final MinecraftAccount account = selectedAccount();
        if (account == null) {
            setStatus(tr("altmanager.status.select_microsoft"), true);
            return;
        }
        if (account.getKind() != AccountKind.MICROSOFT) {
            setStatus(tr("altmanager.status.refresh_microsoft_only"), true);
            return;
        }
        runBackground(tr("altmanager.status.refreshing", account.getUsername()), new AccountTask() {
            @Override
            public MinecraftAccount run() throws Exception {
                return service.refresh(account);
            }
        });
    }

    private void deleteSelected() {
        MinecraftAccount account = selectedAccount();
        if (account == null) {
            setStatus(tr("altmanager.status.select_delete"), true);
            return;
        }
        try {
            service.remove(account.getUuid());
            refreshScreen(tr("altmanager.status.deleted", account.getUsername()), false, null);
        } catch (IOException exception) {
            setStatus(tr("altmanager.status.delete_failed", exception.getMessage()), true);
        }
    }

    private void switchSelected() {
        MinecraftAccount account = selectedAccount();
        if (account == null) {
            setStatus(tr("altmanager.status.select_use"), true);
            return;
        }
        SessionSwitchResult result = service.switchTo(Minecraft.getMinecraft(), account, AltManagerForge112.sessionAdapter());
        if (result.isSuccess()) {
            refreshScreen(tr("altmanager.status.switched", account.getUsername()), false, account.getUuid());
        } else {
            setStatus(tr("altmanager.status.failed", result.getMessage()), true);
        }
    }

    private void runBackground(String startMessage, final AccountTask task) {
        busy = true;
        setStatus(startMessage, false);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final MinecraftAccount account = task.run();
                    Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                        @Override
                        public void run() {
                            refreshScreen(tr("altmanager.status.saved", account.getUsername()), false, account.getUuid());
                        }
                    });
                } catch (final Exception exception) {
                    Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                        @Override
                        public void run() {
                            busy = false;
                            setStatus(tr("altmanager.status.failed", exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage()), true);
                        }
                    });
                }
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
            setStatus(tr("altmanager.status.read_failed", exception.getMessage()), true);
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

    private void refreshScreen(String nextStatus, boolean error, String nextSelectedUuid) {
        this.mc.displayGuiScreen(new AltManagerGui112(parent, nextStatus, error, nextSelectedUuid));
    }

    private void setStatus(String status, boolean error) {
        this.status = status == null ? tr("altmanager.status.ready") : status;
        this.statusError = error;
    }

    private String currentUsername() {
        Minecraft client = Minecraft.getMinecraft();
        return client == null || client.getSession() == null ? "" : client.getSession().getUsername();
    }

    private String expiryText(MinecraftAccount account) {
        Instant expiresAt = account.getAccessTokenExpiresAt();
        return expiresAt == null ? tr("altmanager.label.unknown") : expiresAt.toString();
    }

    private Layout createLayout() {
        Layout next = new Layout();
        int margin = this.width < 520 ? 8 : 14;
        next.compact = this.width < 900 || this.height < 560;
        next.contentTop = this.height < 420 ? 30 : 34;
        next.bottom = Math.max(next.contentTop + 128, this.height - margin);
        next.contentW = next.compact ? Math.max(1, this.width - margin * 2) : Math.min(860, Math.max(1, this.width - margin * 2));
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
        if (this.fontRenderer.getStringWidth(value) <= width) {
            return value;
        }
        String ellipsis = "...";
        int max = Math.max(1, value.length());
        while (max > 0) {
            String candidate = value.substring(0, max) + ellipsis;
            if (this.fontRenderer.getStringWidth(candidate) <= width) {
                return candidate;
            }
            max--;
        }
        return ellipsis;
    }

    private void panel(int x, int y, int width, int height) {
        drawRect(x, y, x + width, y + height, PANEL);
        drawRect(x, y, x + width, y + 1, BORDER);
        drawRect(x, y + height - 1, x + width, y + height, BORDER);
        drawRect(x, y, x + 1, y + height, BORDER);
        drawRect(x + width - 1, y, x + width, y + height, BORDER);
    }

    private static void openUri(String uri) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(uri));
            }
        } catch (Exception ignored) {
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String tr(String key, Object... args) {
        return I18n.format(key, args);
    }

    private static final class Layout {
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
