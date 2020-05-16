package study.querydsl.repository;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;

@Repository
@RequiredArgsConstructor
public class MemberJpaRepository {

	private final EntityManager em;
	private final JPAQueryFactory queryFactory;

	public void save(Member member) {
		em.persist(member);
	}

	public Optional<Member> findById(Long id) {
		Member findMember = em.find(Member.class, id);
		return Optional.ofNullable(findMember);
	}

	public List<Member> findAll() {
		return em.createQuery("select m from Member m", Member.class).getResultList();
	}

	public List<Member> findAll_QUerydsl() {
		return queryFactory
			.select(member)
			.from(member)
			.fetch();
	}


	public List<Member> findAByUserName(String username) {
		return em.createQuery("select m from Member m where m.username = :username", Member.class)
			.setParameter("username", username)
			.getResultList();
	}

	public List<Member> findAByUserName_Querydsl(String username) {
		return queryFactory
			.selectFrom(member)
			.where(member.username.eq(username))
			.fetch();
	}

	public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition) {
		BooleanBuilder builder = new BooleanBuilder();
		if (hasText(condition.getUsername())) {
			builder.and(member.username.eq(condition.getUsername()));
		}

		if (hasText(condition.getTeamName())) {
			builder.and(team.name.eq(condition.getTeamName()));
		}

		if (condition.getAgeGoe() != null) {
			builder.and(member.age.goe(condition.getAgeGoe()));
		}

		if (condition.getAgeLoe() != null) {
			builder.and(member.age.loe(condition.getAgeLoe()));
		}

		return queryFactory
				.select(new QMemberTeamDto(
					member.id.as("memberId"),
					member.username,
					member.age,
					team.id.as("teamId"),
					team.name.as("teamName")))
				.from(member)
				.leftJoin(member.team, team)
				.where(builder)
				.fetch();
	}

	public List<MemberTeamDto> search(MemberSearchCondition condition) {

		return queryFactory
			.select(new QMemberTeamDto(
				member.id.as("memberId"),
				member.username,
				member.age,
				team.id.as("teamId"),
				team.name.as("teamName")))
			.from(member)
			.leftJoin(member.team, team)
			.where(
				userNameEq(condition.getUsername()),
				teamnameEq(condition.getTeamName()),
				ageGoe(condition.getAgeGoe()),
				ageLoe(condition.getAgeLoe())
			 )
			.fetch();
	}

	private BooleanExpression userNameEq(String userName) {
		return hasText(userName) ? member.username.eq(userName) : null;
	}

	private BooleanExpression teamnameEq(String teamName) {
		return hasText(teamName) ? team.name.eq(teamName) : null;
	}

	private BooleanExpression ageGoe(Integer ageGoe) {
		return ageGoe != null ? member.age.goe(ageGoe) : null;
	}

	private BooleanExpression ageLoe(Integer ageLoe) {
		return ageLoe != null ? member.age.loe(ageLoe) : null;
	}


}
