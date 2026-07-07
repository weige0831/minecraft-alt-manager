package dev.codex.altmanager.store;

import dev.codex.altmanager.MinecraftAccount;
import dev.codex.altmanager.util.Uuids;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public interface AccountStore {
    List<MinecraftAccount> load() throws IOException;

    void save(Collection<MinecraftAccount> accounts) throws IOException;

    default void addOrReplace(MinecraftAccount account) throws IOException {
        List<MinecraftAccount> accounts = new ArrayList<MinecraftAccount>(load());
        for (Iterator<MinecraftAccount> iterator = accounts.iterator(); iterator.hasNext(); ) {
            MinecraftAccount existing = iterator.next();
            if (existing.getUuid().equals(account.getUuid()) && existing.getKind() == account.getKind()) {
                iterator.remove();
            }
        }
        accounts.add(account);
        save(accounts);
    }

    default boolean remove(String uuid) throws IOException {
        String normalizedUuid = Uuids.normalize(uuid);
        List<MinecraftAccount> accounts = new ArrayList<MinecraftAccount>(load());
        boolean removed = false;
        for (Iterator<MinecraftAccount> iterator = accounts.iterator(); iterator.hasNext(); ) {
            MinecraftAccount existing = iterator.next();
            if (existing.getUuid().equalsIgnoreCase(normalizedUuid)) {
                iterator.remove();
                removed = true;
            }
        }
        if (removed) {
            save(accounts);
        }
        return removed;
    }
}
