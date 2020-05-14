package mybatis.mapper;

import mybatis.entity.StudentEntity;
import org.apache.ibatis.annotations.Param;

public interface StudentMapper {

	public boolean getStudentById(int id);

	public int addStudent(StudentEntity student);

	public int updateStudentName(@Param("name") String name, @Param("id") int id);

	public StudentEntity getStudentByIdWithClassInfo(int id);

	public StudentEntity findOne(int id);
}
