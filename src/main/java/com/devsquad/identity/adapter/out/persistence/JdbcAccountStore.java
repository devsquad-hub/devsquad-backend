package com.devsquad.identity.adapter.out.persistence;

import com.devsquad.identity.application.ClerkUser;
import com.devsquad.identity.application.port.AccountStore;
import com.devsquad.identity.domain.Account;
import com.devsquad.identity.domain.AccountProfile;
import com.devsquad.shared.persistence.JdbcClient;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class JdbcAccountStore implements AccountStore {

  private final JdbcClient jdbc;

  public JdbcAccountStore(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<Account> findByClerkUserId(String clerkUserId) {
    return jdbc.sql(
            """
            select id, clerk_user_id, email, display_name, avatar_url, bio, skills,
                   github_url, linkedin_url, portfolio_url, availability_hours
            from accounts where clerk_user_id = :clerkUserId and status = 'ACTIVE'
            """)
        .param("clerkUserId", clerkUserId)
        .query(JdbcAccountStore::map)
        .optional();
  }

  @Override
  public Account updateProfile(String clerkUserId, AccountProfile profile) {
    return jdbc.sql(
            """
            update accounts set display_name = :displayName, bio = :bio,
                skills = cast(:skills as text[]), github_url = :githubUrl,
                linkedin_url = :linkedinUrl, portfolio_url = :portfolioUrl,
                availability_hours = :availabilityHours, updated_at = now()
            where clerk_user_id = :clerkUserId and status = 'ACTIVE'
            returning id, clerk_user_id, email, display_name, avatar_url, bio, skills,
                      github_url, linkedin_url, portfolio_url, availability_hours
            """)
        .param("displayName", profile.displayName())
        .param("bio", profile.bio())
        .param("skills", toPgArray(profile.skills()))
        .param("githubUrl", profile.githubUrl())
        .param("linkedinUrl", profile.linkedinUrl())
        .param("portfolioUrl", profile.portfolioUrl())
        .param("availabilityHours", profile.availabilityHours())
        .param("clerkUserId", clerkUserId)
        .query(JdbcAccountStore::map)
        .single();
  }

  @Override
  public void synchronize(ClerkUser user) {
    jdbc.sql(
            """
            insert into accounts (clerk_user_id, email, display_name, avatar_url)
            values (:id, :email, :displayName, :avatarUrl)
            on conflict (clerk_user_id) do update set
                email = excluded.email,
                avatar_url = excluded.avatar_url, status = 'ACTIVE', updated_at = now()
            """)
        .param("id", user.id())
        .param("email", user.primaryEmail())
        .param("displayName", user.displayName())
        .param("avatarUrl", user.imageUrl())
        .update();
  }

  @Override
  public void markDeleted(String clerkUserId) {
    jdbc.sql("update accounts set status = 'DELETED', updated_at = now() where clerk_user_id = :id")
        .param("id", clerkUserId)
        .update();
  }

  private static Account map(ResultSet rs, int rowNumber) throws SQLException {
    var array = rs.getArray("skills");
    var skills = array == null ? List.<String>of() : Arrays.asList((String[]) array.getArray());
    return new Account(
        rs.getObject("id", java.util.UUID.class),
        rs.getString("clerk_user_id"),
        rs.getString("email"),
        rs.getString("display_name"),
        rs.getString("avatar_url"),
        rs.getString("bio"),
        List.copyOf(skills),
        rs.getString("github_url"),
        rs.getString("linkedin_url"),
        rs.getString("portfolio_url"),
        rs.getObject("availability_hours", Integer.class));
  }

  private static String toPgArray(List<String> values) {
    return "{"
        + values.stream()
            .map(value -> "\"" + value.replace("\"", "\\\"") + "\"")
            .reduce((left, right) -> left + "," + right)
            .orElse("")
        + "}";
  }
}
