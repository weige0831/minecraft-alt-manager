package dev.codex.altmanager.forge189;

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

public final class AltManagerGui189 extends GuiScreen {
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
    private String status;
    private boolean statusError;
    private String selectedUuid;
    private List<MinecraftAccount> accounts = new ArrayList<MinecraftAccount>();
    private GuiTextField tokenField;
    private boolean busy;
    private int contentX;
    private int contentTop;
    private int contentBottom;
    private int contentW;
    private int footerY;
    private int bottom;
    private int listX;
    private int listY;
    private int listW;
    private int listBottom;
    private int detailX;
    private int detailW;
    private int rowH;
    private int tokenY;
    private int statusY;
    private int actionY;
    private int navY;
    private boolean compact;

    public AltManagerGui189(GuiScreen parent) {
        this(parent, tr("altmanager.status.ready"), false, null);
    }

    private AltManagerGui189(GuiScreen parent, String status, boolean statusError, String selectedUuid) {
        this.parent = parent;
        this.status = status == null ? tr("altmanager.status.ready") : status;
        this.statusError = statusError;
        this.selectedUuid = selectedUuid;
        this.service = AltManagerForge189.service();
    }

    @Override
    public void initGui() {
        reloadAccounts();
        ensureSelection();
        layout();
        this.buttonList.clear();

        tokenField = new GuiTextField(100, this.fontRendererObj, contentX, tokenY, contentW, 20);
        tokenField.setMaxStringLength(8192);

        MinecraftAccount selected = selectedAccount();
        int gap = 4;

        if (compact) {
            int w = (contentW - gap * 3) / 4;
            addButton(BTN_USE, contentX, actionY, w, tr("altmanager.button.use_short"), selected != null && !busy);
            addButton(BTN_REFRESH, contentX + (w + gap), actionY, w, tr("altmanager.button.refresh_short"), selected != null && selected.getKind() == AccountKind.MICROSOFT && !busy);
            addButton(BTN_DELETE, contentX + (w + gap) * 2, actionY, w, tr("altmanager.button.delete"), selected != null && !busy);
            addButton(BTN_RELOAD, contentX + (w + gap) * 3, actionY, contentW - (w + gap) * 3, tr("altmanager.button.reload"), true);
            int bottomW = (contentW - gap * 2) / 3;
            addButton(BTN_BACK, contentX, navY, bottomW, tr("altmanager.button.back"), true);
            addButton(BTN_MICROSOFT, contentX + bottomW + gap, navY, bottomW, tr("altmanager.button.microsoft_short"), !busy);
            addButton(BTN_IMPORT, contentX + (bottomW + gap) * 2, navY, contentW - (bottomW + gap) * 2, tr("altmanager.button.import_short"), !busy);
            return;
        }

        int actionW = Math.min(MAX_BUTTON_WIDTH, detailW);
        int actionX = detailX + Math.max(0, (detailW - actionW) / 2);
        int halfW = (actionW - gap) / 2;
        addButton(BTN_USE, actionX, actionY, actionW, tr("altmanager.button.use_selected"), selected != null && !busy);
        addButton(BTN_REFRESH, actionX, actionY + 24, halfW, tr("altmanager.button.refresh"), selected != null && selected.getKind() == AccountKind.MICROSOFT && !busy);
        addButton(BTN_DELETE, actionX + halfW + gap, actionY + 24, actionW - halfW - gap, tr("altmanager.button.delete"), selected != null && !busy);
        addButton(BTN_BACK, contentX, navY, 116, tr("altmanager.button.back"), true);
        addButton(BTN_RELOAD, contentX + 122, navY, 104, tr("altmanager.button.reload"), true);
        addButton(BTN_MICROSOFT, contentX + contentW - 268, navY, 126, tr("altmanager.button.microsoft"), !busy);
        addButton(BTN_IMPORT, contentX + contentW - 136, navY, 136, tr("altmanager.button.import_token"), !busy);
    }

    private void addButton(int id, int x, int y, int width, String label, boolean enabled) {
        GuiButton button = new GuiButton(id, x, y, Math.min(MAX_BUTTON_WIDTH, Math.max(36, width)), 20, label);
        button.enabled = enabled;
        this.buttonList.add(button);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, this.width, this.height, 0xF0101117);
        drawRect(0, 0, this.width, 26, 0xFF151A24);
        drawCenteredString(this.fontRendererObj, tr("altmanager.screen.title"), this.width / 2, 9, TEXT);

        if (compact) {
            panel(contentX, contentTop, contentW, Math.max(40, contentBottom - contentTop));
            panel(contentX, footerY, contentW, Math.max(42, bottom - footerY));
            drawString(this.fontRendererObj, tr("altmanager.section.saved_accounts"), contentX + 10, contentTop + 8, TEXT);
            drawRightAligned(tr("altmanager.label.total", accounts.size()), contentX + contentW - 10, contentTop + 8, MUTED);
            drawCompactDetails(contentX + 10, contentTop + 28, contentW - 20);
            drawAccounts(mouseX, mouseY);
        } else {
            panel(listX, contentTop, listW, Math.max(40, contentBottom - contentTop));
            panel(detailX, contentTop, detailW, Math.max(40, contentBottom - contentTop));
            panel(contentX, footerY, contentW, Math.max(42, bottom - footerY));
            drawString(this.fontRendererObj, tr("altmanager.section.saved_accounts"), listX + 10, contentTop + 9, TEXT);
            drawRightAligned(tr("altmanager.label.total", accounts.size()), listX + listW - 10, contentTop + 9, MUTED);
            drawAccounts(mouseX, mouseY);
            drawDetails(detailX, contentTop, detailW);
        }

        drawString(this.fontRendererObj, fit(status, contentW - 16), contentX + 8, statusY, busy ? WARNING : (statusError ? DANGER : SUCCESS));
        tokenField.drawTextBox();
        if (tokenField.getText().isEmpty() && !tokenField.isFocused()) {
            drawString(this.fontRendererObj, tr("altmanager.placeholder.access_token"), contentX + 4, tokenY + 6, MUTED);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawAccounts(int mouseX, int mouseY) {
        if (accounts.isEmpty()) {
            drawString(this.fontRendererObj, tr("altmanager.empty.no_accounts"), listX + 10, listY + 12, MUTED);
            return;
        }
        int y = listY;
        String currentName = currentUsername();
        for (MinecraftAccount account : accounts) {
            if (y + rowH > listBottom - 6) {
                drawString(this.fontRendererObj, "...", listX + 12, y + 10, MUTED);
                break;
            }
            boolean selected = account.getUuid().equals(selectedUuid);
            boolean current = account.getUsername().equals(currentName);
            boolean hover = mouseX >= listX + 6 && mouseX <= listX + listW - 6 && mouseY >= y && mouseY <= y + rowH - 3;
            drawRect(listX + 6, y, listX + listW - 6, y + rowH - 3, selected ? 0xFF243B5C : (hover ? 0xFF202838 : PANEL_DARK));
            if (selected) {
                drawRect(listX + 6, y, listX + 9, y + rowH - 3, ACCENT);
            }
            drawString(this.fontRendererObj, fit(account.getUsername(), listW - 84), listX + 15, y + 5, TEXT);
            drawString(this.fontRendererObj, current ? tr("altmanager.badge.current") : account.getKind().name(), listX + 15, y + 18, current ? SUCCESS : MUTED);
            y += rowH;
        }
    }

    private void drawDetails(int x, int top, int width) {
        MinecraftAccount account = selectedAccount();
        drawString(this.fontRendererObj, tr("altmanager.section.selected_account"), x + 10, top + 9, TEXT);
        if (account == null) {
            drawString(this.fontRendererObj, tr("altmanager.empty.select_account"), x + 10, top + 38, MUTED);
            drawString(this.fontRendererObj, tr("altmanager.empty.add_account"), x + 10, top + 52, MUTED);
            return;
        }
        int y = top + 38;
        drawField(x + 10, y, tr("altmanager.label.name"), account.getUsername(), width - 20);
        y += 28;
        drawField(x + 10, y, tr("altmanager.label.uuid"), account.getUuid(), width - 20);
        y += 28;
        drawField(x + 10, y, tr("altmanager.label.source"), account.getKind().name(), width - 20);
        y += 28;
        drawField(x + 10, y, tr("altmanager.label.expires"), expiryText(account), width - 20);
        y += 34;
        boolean current = account.getUsername().equals(currentUsername());
        drawString(this.fontRendererObj, fit(tr(current ? "altmanager.status.active_session" : "altmanager.status.ready_to_use"), width - 20), x + 10, y, current ? SUCCESS : MUTED);
        drawString(this.fontRendererObj, fit(tr("altmanager.label.action_hint"), width - 20), x + 10, actionY - 16, MUTED);
    }

    private void drawCompactDetails(int x, int y, int width) {
        MinecraftAccount account = selectedAccount();
        if (account == null) {
            drawString(this.fontRendererObj, tr("altmanager.empty.add_account"), x, y, MUTED);
            return;
        }
        drawString(this.fontRendererObj, fit(account.getUsername(), width), x, y, TEXT);
        drawString(this.fontRendererObj, fit(account.getKind().name() + " | " + expiryText(account), width), x, y + 12, MUTED);
    }

    private void drawField(int x, int y, String label, String value, int width) {
        drawString(this.fontRendererObj, label, x, y, MUTED);
        drawString(this.fontRendererObj, fit(value == null ? "-" : value, width), x, y + 11, TEXT);
    }

    private void drawRightAligned(String text, int right, int y, int color) {
        drawString(this.fontRendererObj, text, right - this.fontRendererObj.getStringWidth(text), y, color);
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

    private void loginAccessToken() {
        String token = AccessTokenSanitizer.extract(tokenField.getText());
        if (busy || token.isEmpty()) {
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
        if (!AltManagerForge189.config().hasMicrosoftClientId()) {
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
        if (account == null || account.getKind() != AccountKind.MICROSOFT) {
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
        SessionSwitchResult result = service.switchTo(Minecraft.getMinecraft(), account, AltManagerForge189.sessionAdapter());
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
        this.mc.displayGuiScreen(new AltManagerGui189(parent, nextStatus, error, nextSelectedUuid));
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

    private void layout() {
        int margin = this.width < 520 ? 8 : 14;
        compact = this.width < 900 || this.height < 560;
        contentTop = this.height < 420 ? 30 : 34;
        bottom = Math.max(contentTop + 128, this.height - margin);
        int availableH = Math.max(120, bottom - contentTop);
        int footerH = compact ? clamp(availableH / 3, 90, 106) : 66;
        footerY = bottom - footerH;
        contentW = Math.min(compact ? this.width - margin * 2 : 760, Math.max(1, this.width - margin * 2));
        contentX = (this.width - contentW) / 2;
        contentBottom = Math.max(contentTop + 72, footerY - 8);
        if (contentBottom > footerY - 4) {
            contentBottom = footerY - 4;
        }
        tokenY = compact ? footerY + 22 : footerY + 6;
        statusY = compact ? footerY + 7 : footerY + 31;
        actionY = compact ? footerY + 46 : Math.max(contentTop + 110, contentBottom - 58);
        navY = compact ? footerY + 70 : bottom - 24;
        if (compact) {
            rowH = 30;
            listX = contentX;
            listY = contentTop + 56;
            listW = contentW;
            listBottom = Math.max(listY + rowH, contentBottom);
            detailX = contentX;
            detailW = contentW;
        } else {
            rowH = 32;
            listX = contentX;
            listY = contentTop + 28;
            listW = clamp((contentW * 42) / 100, 280, 320);
            listBottom = Math.max(listY + rowH, contentBottom);
            detailX = contentX + listW + 10;
            detailW = contentW - listW - 10;
        }
    }

    private String fit(String value, int width) {
        if (value == null || width <= 0) {
            return "";
        }
        if (this.fontRendererObj.getStringWidth(value) <= width) {
            return value;
        }
        String ellipsis = "...";
        int max = value.length();
        while (max > 0) {
            String candidate = value.substring(0, max) + ellipsis;
            if (this.fontRendererObj.getStringWidth(candidate) <= width) {
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

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void openUri(String uri) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(uri));
            }
        } catch (Exception ignored) {
        }
    }

    private static String tr(String key, Object... args) {
        return I18n.format(key, args);
    }

    private interface AccountTask {
        MinecraftAccount run() throws Exception;
    }
}
