package im.kagami.comment;

import tools.jackson.databind.ObjectMapper;
import im.kagami.comment.domain.Comment;
import im.kagami.comment.mapper.CommentMapper;
import im.kagami.comment.dto.CreateCommentRequest;
import im.kagami.post.domain.Post;
import im.kagami.post.dto.CreatePostRequest;
import im.kagami.post.mapper.PostMapper;
import im.kagami.post.service.PostService;
import im.kagami.security.jwt.JwtTokenProvider;
import im.kagami.user.domain.User;
import im.kagami.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API tests for the comment endpoints, running against a real PostgreSQL via Testcontainers
 * (Flyway applies every migration on the fresh container). The visitor JWTs are minted directly
 * with the production token provider instead of going through /api/login, so the login rate limit
 * cannot interfere with the assertions.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
// Every test rolls back, so tests stay isolated without manual cleanup.
@Transactional
class CommentApiControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18.4");

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    JwtTokenProvider tokenProvider;
    @Autowired
    UserMapper userMapper;
    @Autowired
    PostMapper postMapper;
    @Autowired
    PostService postService;
    @Autowired
    CommentMapper commentMapper;

    private static final AtomicInteger SEQ = new AtomicInteger();

    Long authorId;
    Long visitorId;
    Long postId;
    String authorToken;
    String visitorToken;

    @BeforeEach
    void setUp() {
        String suffix = String.valueOf(SEQ.incrementAndGet());
        authorId = createUser("author-" + suffix, "Author " + suffix);
        visitorId = createUser("visitor-" + suffix, "Visitor " + suffix);
        authorToken = tokenProvider.generateToken(authorId, "author-" + suffix);
        visitorToken = tokenProvider.generateToken(visitorId, "visitor-" + suffix);
        postId = postService.create(
                authorId,
                new CreatePostRequest("Test post " + suffix, null, null, null, "Test content", false, List.<String>of())
        ).id();
        postService.publish(postId, authorId);
    }

    @Test
    void anonymousCommentingIsRejected() throws Exception {
        mockMvc.perform(post("/api/posts/" + postId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommentRequest("Hello", null))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void signedInVisitorCreatesTopLevelComment() throws Exception {
        mockMvc.perform(post("/api/posts/" + postId + "/comments")
                        .header("Authorization", bearer(visitorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommentRequest("Nice post!", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").value("Nice post!"))
                .andExpect(jsonPath("$.data.replies.length()").value(0));

        assertThat(commentMapper.selectList(null)).hasSize(1);
    }

    @Test
    void threadsKeepTopLevelAndRepliesApart() throws Exception {
        Long topLevel = createComment(visitorToken, "Top-level question", null);
        Long reply = createComment(authorToken, "Author reply", topLevel);
        createComment(authorToken, "Second reply", topLevel);

        String body = mockMvc.perform(get("/api/posts/" + postId + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(topLevel))
                .andExpect(jsonPath("$.data[0].replies.length()").value(2))
                .andExpect(jsonPath("$.data[0].replies[0].id").value(reply))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("Second top-level");
        Long secondTopLevel = createComment(visitorToken, "Second top-level", null);
        mockMvc.perform(get("/api/posts/" + postId + "/comments"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[1].replies.length()").value(0));

        assertThat(commentMapper.selectById(secondTopLevel)).isNotNull();
    }

    @Test
    void replyingToAReplyIsRejected() throws Exception {
        Long topLevel = createComment(visitorToken, "Top-level", null);
        Long reply = createComment(authorToken, "Reply", topLevel);

        mockMvc.perform(post("/api/posts/" + postId + "/comments")
                        .header("Authorization", bearer(authorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommentRequest("Nested!", reply))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(90002));
    }

    @Test
    void commentingOnUnknownOrUnpublishedPostIs404() throws Exception {
        mockMvc.perform(post("/api/posts/999999/comments")
                        .header("Authorization", bearer(visitorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommentRequest("Hi", null))))
                .andExpect(status().isNotFound());

        Long draftId = postService.create(
                authorId,
                new CreatePostRequest("Draft post", null, null, null, "Draft content", false, List.<String>of())
        ).id();
        mockMvc.perform(post("/api/posts/" + draftId + "/comments")
                        .header("Authorization", bearer(visitorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommentRequest("Hi", null))))
                .andExpect(status().isNotFound());
    }

    @Test
    void commentContentIsValidated() throws Exception {
        mockMvc.perform(post("/api/posts/" + postId + "/comments")
                        .header("Authorization", bearer(visitorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommentRequest("   ", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(90002));

        mockMvc.perform(post("/api/posts/" + postId + "/comments")
                        .header("Authorization", bearer(visitorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommentRequest("x".repeat(1001), null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void onlyThePostAuthorCanHideAndUnhide() throws Exception {
        Long commentId = createComment(visitorToken, "To be hidden", null);

        mockMvc.perform(post("/api/manage/comments/" + commentId + "/hide")
                        .header("Authorization", bearer(visitorToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/manage/comments/" + commentId + "/hide")
                        .header("Authorization", bearer(authorToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts/" + postId + "/comments"))
                .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(get("/api/manage/comments?postId=" + postId)
                        .header("Authorization", bearer(authorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value(1));

        mockMvc.perform(post("/api/manage/comments/" + commentId + "/unhide")
                        .header("Authorization", bearer(authorToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts/" + postId + "/comments"))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void deletionFollowsTheAuthorRules() throws Exception {
        Long visitorsComment = createComment(visitorToken, "Visitor comment", null);
        Long authorsComment = createComment(authorToken, "Author comment", null);

        // A stranger may not delete
        mockMvc.perform(delete("/api/manage/comments/" + visitorsComment)
                        .header("Authorization", bearer(tokenFor("stranger-deletion"))))
                .andExpect(status().isForbidden());

        // The comment's own author may delete
        mockMvc.perform(delete("/api/manage/comments/" + visitorsComment)
                        .header("Authorization", bearer(visitorToken)))
                .andExpect(status().isOk());

        // The post's author may delete
        mockMvc.perform(delete("/api/manage/comments/" + authorsComment)
                        .header("Authorization", bearer(authorToken)))
                .andExpect(status().isOk());

        assertThat(commentMapper.selectList(null)).isEmpty();
    }

    @Test
    void deletingATopLevelCommentTakesItsReplies() throws Exception {
        Long topLevel = createComment(visitorToken, "Top-level", null);
        createComment(authorToken, "Reply", topLevel);

        mockMvc.perform(delete("/api/manage/comments/" + topLevel)
                        .header("Authorization", bearer(authorToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts/" + postId + "/comments"))
                .andExpect(jsonPath("$.data.length()").value(0));

        assertThat(commentMapper.selectList(null)).isEmpty();
    }

    // ---------- helpers ----------

    private Long createUser(String username, String nickname) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setNickname(nickname);
        user.setStatus(0);
        userMapper.insert(user);
        return user.getId();
    }

    private String tokenFor(String username) {
        // Only used where a standalone stranger identity is needed; the user row is created here.
        Long id = createUser(username, username);
        return tokenProvider.generateToken(id, username);
    }

    private Long createComment(String token, String content, Long parentId) throws Exception {
        String body = mockMvc.perform(post("/api/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommentRequest(content, parentId))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("id").asLong();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
