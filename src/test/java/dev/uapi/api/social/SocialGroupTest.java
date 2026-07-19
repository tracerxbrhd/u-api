package dev.uapi.api.social;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class SocialGroupTest {
    @Test
    void snapshotsDefensivelyCopyMembersAndRoleFlags() {
        UUID playerId = UUID.randomUUID();
        EnumSet<SocialGroupRoleFlag> flags = EnumSet.of(
            SocialGroupRoleFlag.MEMBER, SocialGroupRoleFlag.LEADER);
        SocialGroupMember member = new SocialGroupMember(
            playerId, "Player", Optional.of("party_leader"), flags, true);
        List<SocialGroupMember> members = new ArrayList<>(List.of(member));
        SocialGroup group = new SocialGroup(
            UUID.randomUUID(), SocialGroupType.PARTY, "Party", 3, members);

        flags.clear();
        members.clear();

        assertTrue(member.isLeader());
        assertEquals(List.of(member), group.members());
        assertEquals(Optional.of(member), group.member(playerId));
        assertThrows(UnsupportedOperationException.class,
            () -> member.roleFlags().add(SocialGroupRoleFlag.ASSISTANT));
        assertThrows(UnsupportedOperationException.class,
            () -> group.members().clear());
    }

    @Test
    void rejectsDuplicateMembers() {
        UUID playerId = UUID.randomUUID();
        SocialGroupMember first = member(playerId, "First");
        SocialGroupMember second = member(playerId, "Second");

        assertThrows(IllegalArgumentException.class, () -> new SocialGroup(
            UUID.randomUUID(), SocialGroupType.GUILD, "Guild", 0, List.of(first, second)));
    }

    @Test
    void rejectsAnEmptyActiveGroupSnapshot() {
        assertThrows(IllegalArgumentException.class, () -> new SocialGroup(
            UUID.randomUUID(), SocialGroupType.PARTY, "Party", 0, List.of()));
    }

    private static SocialGroupMember member(UUID playerId, String name) {
        return new SocialGroupMember(playerId, name, Optional.empty(),
            EnumSet.of(SocialGroupRoleFlag.MEMBER), true);
    }
}
