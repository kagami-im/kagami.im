package im.kagami.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import im.kagami.post.domain.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PostMapper extends BaseMapper<Post> {

    /**
     * Page of published posts, newest first, optionally filtered by tag name. The optional tag
     * filter joins through post_tags, so it lives in XML; paging is applied by the pagination
     * interceptor ({@code MybatisPlusConfig}) because the {@link Page} argument is the first one.
     */
    Page<Post> selectPublishedPage(Page<Post> page, @Param("tagName") String tagName);
}
