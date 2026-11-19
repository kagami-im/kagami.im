package im.kagami.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import im.kagami.post.domain.Tag;
import im.kagami.post.domain.TagCountRow;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    /** Every tag with the number of published posts carrying it, busiest first. */
    List<TagCountRow> selectTagCounts();
}
