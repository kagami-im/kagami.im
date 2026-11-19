package im.kagami.post.service;

import im.kagami.post.domain.PostTagRow;
import im.kagami.post.domain.Tag;
import im.kagami.post.domain.TagCountRow;
import im.kagami.post.mapper.PostTagMapper;
import im.kagami.post.mapper.TagMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagMapper tagMapper;
    private final PostTagMapper postTagMapper;

    /** Every tag with its published-post count, for the tag page. */
    public List<TagCountRow> listTagCounts() {
        return tagMapper.selectTagCounts();
    }

    /**
     * Tags of the given posts in one query, grouped by post id. Empty input short-circuits to an
     * empty map because a SQL {@code IN ()} is illegal.
     */
    public Map<Long, List<Tag>> tagsByPostIds(Collection<Long> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<Tag>> result = new HashMap<>();
        for (PostTagRow row : postTagMapper.selectByPostIds(postIds)) {
            Tag tag = new Tag();
            tag.setId(row.getTagId());
            tag.setName(row.getTagName());
            result.computeIfAbsent(row.getPostId(), key -> new ArrayList<>()).add(tag);
        }
        return result;
    }

    /**
     * Rewires a post's tags to exactly the requested set: normalizes the names, creates the ones
     * that do not exist yet, then replaces the link rows. Rows removed here (and link rows of
     * deleted posts) are also cleaned up by the FK cascade.
     */
    @Transactional
    public void replacePostTags(Long postId, List<String> requestedNames) {
        List<String> names = normalize(requestedNames);
        postTagMapper.deleteByPostId(postId);
        if (names.isEmpty()) {
            return;
        }
        postTagMapper.insertBatch(postId, findOrCreateByNames(names).stream().map(Tag::getId).toList());
    }

    /** Trims, drops blanks and collapses duplicates while keeping the requested order. */
    private List<String> normalize(List<String> requestedNames) {
        if (requestedNames == null) {
            return List.of();
        }
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (String name : requestedNames) {
            if (name != null && !name.isBlank()) {
                names.add(name.trim());
            }
        }
        return List.copyOf(names);
    }

    /**
     * Existing tags by name, missing ones inserted; the result follows the requested order.
     * <p>Known limitation: two authors creating the same brand-new tag concurrently will hit the
     * unique constraint on tags.name and one request fails with a system error. Acceptable at this
     * scale; the fix would be catching the duplicate-key exception and re-selecting.
     */
    private List<Tag> findOrCreateByNames(List<String> names) {
        Map<String, Tag> existing = tagMapper
                .selectList(new LambdaQueryWrapper<Tag>().in(Tag::getName, names))
                .stream()
                .collect(Collectors.toMap(Tag::getName, tag -> tag));
        List<Tag> result = new ArrayList<>(names.size());
        for (String name : names) {
            Tag tag = existing.get(name);
            if (tag == null) {
                tag = new Tag();
                tag.setName(name);
                tagMapper.insert(tag);
            }
            result.add(tag);
        }
        return result;
    }
}
