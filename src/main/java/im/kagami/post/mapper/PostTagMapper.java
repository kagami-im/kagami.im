package im.kagami.post.mapper;

import im.kagami.post.domain.PostTagRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * Link-table access for post_tags. It has no single-column primary key, so it stays a plain mapper
 * (no BaseMapper) and every statement is written by hand in the XML.
 */
@Mapper
public interface PostTagMapper {

    void deleteByPostId(@Param("postId") Long postId);

    /** The callers guarantee {@code tagIds} is non-empty. */
    void insertBatch(@Param("postId") Long postId, @Param("tagIds") List<Long> tagIds);

    /** Tags of the given posts in one query, as flat (postId, tagId, tagName) rows; non-empty input only. */
    List<PostTagRow> selectByPostIds(@Param("postIds") Collection<Long> postIds);
}
