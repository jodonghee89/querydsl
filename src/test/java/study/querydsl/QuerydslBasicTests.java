package study.querydsl;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

@Slf4j
@SpringBootTest
@Transactional
public class QuerydslBasicTests {

	@Autowired
	EntityManager em;

	JPAQueryFactory queryFactory;

	BooleanBuilder builder;

	public QuerydslBasicTests() {
		System.out.println("------------------------");
		System.out.println(queryFactory);
		System.out.println("------------------------");
	}

	@BeforeEach
	public void setting() {
		queryFactory = new JPAQueryFactory(em);
		System.out.println("---------- start settings method ---------");
		Team teamA = new Team("teamA");
		Team teamB = new Team("teamB");
		em.persist(teamA);
		em.persist(teamB);

		Member member1 = new Member("member1", 10, teamA);
		Member member2 = new Member("member2", 20, teamA);
		em.persist(member1);
		em.persist(member2);

		Member member3 = new Member("member3", 30, teamB);
		Member member4 = new Member("member4", 40, teamB);
		em.persist(member3);
		em.persist(member4);

		em.flush();
		System.out.println("---------- end settings method ---------");
	}

	@Test
	public void startJPQL() {
		//member1 search
		Member findMember = em
			.createQuery("select m from Member m where m.username = :username", Member.class)
			.setParameter("username", "member1")
			.getSingleResult();

		System.out.println(findMember);
		System.out.println("============1============");

		Stream<Member> findMember1 = em
			.createQuery("select m from Member m where m.username = :username", Member.class)
			.setParameter("username", "member1")
			.getResultStream();
		System.out.println(findMember1);
		System.out.println("=============2===========");
		findMember1.sorted().forEach(System.out::println);
		System.out.println("=============3===========");

		assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	public void startQuerydsl() {
		QMember m = new QMember("m");

		System.out.println("123123");
		System.out.println("------------------------");
		System.out.println(queryFactory);
		System.out.println("------------------------");

		System.out.println(queryFactory.select(m).from(m));
		Member findMember2 =
			queryFactory.
				selectFrom(m).
				where(m.username.eq("member1")).fetchOne(); // 파라미터 바인딩 처리
		assertThat(findMember2.getUsername()).isEqualTo("member1");
	}

	@Test
	public void search() {
		Member findMember = queryFactory
			.selectFrom(member)
			.where(member.username.eq("member1").and(member.age.eq(10)))
			.fetchOne();

		assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	public void searchAndParam() {
		Member findMember = queryFactory
			.selectFrom(member)
			.where(
				member.username.eq("member1"),
				member.age.eq(10)
			)
			.fetchOne();

		assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	public void searchFetch() {
//		List<Member> resultList = queryFactory.selectFrom(member).fetch();
//
//		Member result = queryFactory.selectFrom(member).fetchOne();
//
//		Member result1 = queryFactory.selectFrom(member).fetchFirst();

//		QueryResults<Member> queryResults = queryFactory.selectFrom(member).fetchResults();
//		long total = queryResults.getTotal();
//		List<Member> results11 = queryResults.getResults();

		long count = queryFactory.selectFrom(member).fetchCount();
	}

	@Test
	public void searchOrderBy() {
		em.persist(new Member(null, 200));
		em.persist(new Member("member13", 200));
		em.persist(new Member("member14", 200));

		List<Member> memberList = queryFactory
			.selectFrom(member)
			.orderBy(member.age.desc(), member.username.asc().nullsLast())
			.fetch();

		System.out.println("================ start ===================");
		System.out.println(memberList.size());
		for(Member m : memberList) {
			System.out.println("member.toString >> " + m.toString());
		}
		System.out.println("================ end ===================");
	}

	@Test
	public void paging() {
		Tuple tuple = queryFactory
			.select(
				member.count(),
				member.age.avg(),
				member.age.sum(),
				member.age.min(),
				member.age.max()
			)
			.from(member)
			.where(member.username.eq("member1"))
			.fetchOne();

		assertThat(tuple.get(member.age.avg())).isEqualTo(10);
		assertThat(tuple.get(member.count())).isEqualTo(1);
	}

	@Test
	public void join() {
		String teamName = queryFactory
			.select(team.name)
			.from(member)
			.join(member.team, team)
			.where(member.username.eq("member1"))
			.fetchOne();

		System.out.println("teamName >>>>> " + teamName);
	}

	@Test
	public void join1() {
		List<Member> resultList = queryFactory
			.selectFrom(member)
			.join(member.team, team)
			.where(team.name.eq("teamA"))
			.fetch();


		assertThat(resultList)
			.extracting("username")
			.containsExactly("member1", "member2");
	}

	@Test
	public void theta_join() {
		em.persist(new Member("teamA"));
		em.persist(new Member("teamB"));
		em.persist(new Member("teamC"));

		em.flush();

		List<Member> result = queryFactory
			.select(member)
			.from(member, team)
			.where(member.username.eq(team.name))
			.fetch();

		assertThat(result)
			.extracting("username")
			.containsExactly("teamA", "teamB");
	}

	@Test
	public void join_on_filtering() {
		List<Tuple> resultList = queryFactory
			.select(member, team)
			.from(member)
			.join(member.team, team).on(team.name.eq("teamA"))
			.fetch();

//		List<Tuple> resultList = queryFactory
//			.select(member, team)
//			.from(member)
//			.leftJoin(member.team, team)
//			.where(team.name.eq("teamA"))
//			.fetch();

		for (Tuple tuple : resultList) {
			System.out.println("tuple >>> " + tuple);
		}
	}

	@Test
	public void join_on_no_relation() {
		em.persist(new Member("teamA"));
		em.persist(new Member("teamB"));
		em.persist(new Member("teamC"));

		em.flush();

		List<Tuple> resultList = queryFactory
			.select(member, team)
			.from(member)
			.leftJoin(team).on(member.username.eq(team.name))
			.fetch();

		for (Tuple tuple : resultList) {
			System.out.println("tuple >>> " + tuple);
		}
	}

	@PersistenceUnit
	EntityManagerFactory emf;

	@Test
	public void fetchJoinNo() {
		em.clear();

		Member result = queryFactory
			.selectFrom(member)
			.where(member.username.eq("member1"))
			.fetchOne();

		boolean loaded = emf.getPersistenceUnitUtil().isLoaded(result.getTeam());
		assertThat(loaded).as("페치 조인 미적용").isFalse();
	}

	@Test
	public void fetchJoinUse() {
		em.clear();

		Member result = queryFactory
			.selectFrom(member)
			.join(member.team, team).fetchJoin()
			.where(member.username.eq("member1"))
			.fetchOne();

		boolean loaded = emf.getPersistenceUnitUtil().isLoaded(result.getTeam());
		assertThat(loaded).as("페치 조인 미적용").isTrue();
	}

	@Test
	public void subQuery() {

		QMember member1 = new QMember("memberSub");
		List<Member> result = queryFactory
			.selectFrom(member)
			.where(member.age.eq(
				JPAExpressions
					.select(member1.age.max())
					.from(member1)
			))
			.fetch();

		assertThat(result).extracting("age", Integer.class).containsExactly(40);
	}

	@Test
	public void subQueryGoe() {

		QMember member1 = new QMember("memberSub");
		List<Member> result = queryFactory
			.selectFrom(member)
			.where(member.age.goe(
				JPAExpressions
					.select(member1.age.avg())
					.from(member1)
			))
			.fetch();

		assertThat(result).extracting("age", Integer.class).containsExactly(30,40);
	}

	@Test
	public void subQueryIn() {

		QMember member1 = new QMember("memberSub");
		List<Member> result = queryFactory
			.selectFrom(member)
			.where(member.age.in(
				JPAExpressions
					.select(member1.age)
					.from(member1)
					.where(member1.age.gt(10))
			))
			.fetch();

		assertThat(result).extracting("age", Integer.class).containsExactly(30,40);
	}

	@Test
	public void selectSubquery() {

		QMember member1 = new QMember("memberSub");

		List<Tuple> result = queryFactory
			.select(member.username,
				Expressions.as(
					JPAExpressions.select(member1.age.avg())
						.from(member1), "age"
				)

			)
			.from(member)
			.fetch();

			result
			.forEach(a -> {
				System.out.println(a.get(member.username));
				System.out.println(a.get(Expressions.numberPath(Double.class, "age")));
				System.out.println(a.get(Expressions.numberPath(Double.class, "age")).getClass().getName());
				System.out.println(a.get(Expressions.path(Double.class, "age")));
				System.out.println(a.get(Expressions.path(Double.class, "age")).getClass().getName());
			});
	}

	@Test
	public void basicCase() {
		List<String> fetch = queryFactory
			.select(member.age
				.when(10).then("열살")
				.when(20).then("스무살")
				.otherwise("기타")
			)
			.from(member)
			.fetch();

		for (String s : fetch) {
			System.out.println(String.format("s = %s", s));
		}
	}

	@Test
	public void complexCase() {
		List<String> fetch = queryFactory
			.select(new CaseBuilder()
				.when(member.age.between(0, 20)).then("0~20살")
				.when(member.age.between(21, 30)).then("21~30살")
				.otherwise("기타"))
			.from(member)
			.fetch();

		for (String s : fetch) {
			System.out.println(String.format("s = %s", s));
		}
	}

	@Test
	public void constant() {
		List<Tuple> result = queryFactory
			.select(member.username, Expressions.constant("A"))
			.from(member)
			.fetch();

		for (Tuple tuple : result) {
			System.out.println("tuple = " + tuple);
		}
	}

	@Test
	public void concat() {
//		List<String> stringList = queryFactory
//			.select(member.username.concat("_").concat(String.valueOf(member.age)))
//			.from(member)
//			.fetch();

		List<String> stringList = queryFactory
			.select(member.username.concat("_").concat(member.age.stringValue()))
			.from(member)
			.fetch();

		for (String s : stringList) {
			System.out.println("s = " + s);
		}
	}

	@Test
	public void simpleProjection() {
		List<String> result = queryFactory
			.select(member.username)
			.from(member)
			.fetch();

		for (String s : result) {
			System.out.println("s = " + s);
		}
	}

	@Test
	public void tupleProjection() {
		List<Tuple> result = queryFactory
			.select(member.username, member.age)
			.from(member)
			.fetch();

		for (Tuple tuple : result) {
			String s = tuple.get(member.username);
			Integer a = tuple.get(member.age);
		}
	}

	@Test
	public void findDtoByJPQL() {
		List<MemberDto> resultList = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
			.getResultList(); //뉴 오퍼레이션 활용 방법  JQTL

		for (MemberDto memberDto : resultList) {
			System.out.println("memberDto = " + memberDto);
		}
	}

	@Test
	public void findDtoBySetter() { //querydsl setter를 통해 필드에 값 셋팅
		List<MemberDto> resultList = queryFactory
			.select(Projections.bean(MemberDto.class,
				member.username,
				member.age))
			.from(member)
			.fetch();

		for (MemberDto memberDto : resultList) {
			System.out.println("memberDto = " + memberDto);
		}
	}

	@Test
	public void findDtoByField() { //querydsl 필드에 곧장 값 셋팅
		List<MemberDto> resultList = queryFactory
			.select(Projections.fields(MemberDto.class,
				member.username,
				member.age))
			.from(member)
			.fetch();

		for (MemberDto memberDto : resultList) {
			System.out.println("memberDto = " + memberDto);
		}
	}

	@Test
	public void findDtoByConstructor() { //querydsl 생성자로 값 설정
		List<UserDto> resultList = queryFactory
			.select(Projections.constructor(UserDto.class,
				member.username,
				member.age))
			.from(member)
			.fetch();

		for (UserDto memberDto : resultList) {
			System.out.println("memberDto = " + memberDto.toString());
		}
	}

	@Test
	public void findUserDto() { //querydsl 생성자로 값 설정

		QMember memberSub = new QMember("memberSub");
		List<UserDto> resultList = queryFactory
			.select(Projections.fields(UserDto.class,
				member.username.as("name"),
				ExpressionUtils.as(JPAExpressions
							.select(memberSub.age.max()).from(memberSub), "age")))
			.from(member)
			.fetch();

		for (UserDto userDto : resultList) {
			System.out.println("userDto = " + userDto);
		}
	}

	@Test
	public void findDtoByQueryProjection() {
		List<MemberDto> resultList = queryFactory
			.select(new QMemberDto(member.username, member.age))
			.from(member)
			.fetch();

		for (MemberDto memberDto : resultList) {
			System.out.println("memberDto = " + memberDto);
		}
	}

	@Test
	public void dynamicQuery_BooleanBuilder() {
		String usernameParam = "member1";
		Integer ageParam = null;

		List<Member> result = searchMember1(usernameParam, ageParam);
		assertThat(result.size()).isEqualTo(1);
	}

	private List<Member> searchMember1(String usernameCond, Integer ageCond) {

		builder = new BooleanBuilder();
		if (usernameCond != null) {
			builder.and(member.username.eq(usernameCond));
		}

		if (ageCond != null) {
			builder.and(member.age.eq(ageCond));
		}

		System.out.println("------------------------------");
		System.out.println("builder :: " + builder);
		System.out.println("------------------------------");

		return queryFactory
			.select(member)
			.from(member)
			.where(builder)
			.fetch();
	}

	@Test
	public void dynamicQUery_WhereParam() {
		String usernameParam = null;
		Integer ageParam = 10;

		List<Member> result = searchMember2(usernameParam, ageParam);
		assertThat(result.size()).isEqualTo(1);
	}

	private List<Member> searchMember2(String usernameCond, Integer ageCond) {

//		return queryFactory
//			.selectFrom(member)
//			.where(usernameEq(usernameCond), ageEq(ageCond))
//			.fetch();

		return queryFactory
			.selectFrom(member)
			.where(allEq(usernameCond, ageCond))
//			.where(usernameEq(usernameCond))
			.fetch();
	}

	private BooleanExpression usernameEq(String usernameCond) {
		return usernameCond == null ? null : member.username.eq(usernameCond);
	}

	private BooleanExpression ageEq(Integer ageCond) {
		return ageCond == null ? null : member.age.eq(ageCond);
	}

	private BooleanExpression allEq(String userNameCond, Integer ageCond) {
		System.out.println("======================");
		System.out.println(usernameEq(userNameCond).and(ageEq(ageCond)));
		System.out.println("======================");
		return usernameEq(userNameCond).and(ageEq(ageCond));
	}

	@Test
	public void bulkUpdate() {

		/*
		* 벌크 업데이트는 영속성 컨텍스트를 거치지 않고 바로 디비에 업데이트함
		* ex)
		* */

		long count = queryFactory
			.update(member)
			.set(member.username, "비회원")
			.where(member.age.lt(28))
			.execute();

		em.clear();
		// 업데이트 후

		// 영속성 컨텍스트
		// member1 = 10 -> member1
		// member2 = 20 -> member2
		// member3 = 30 -> member3
		// member4 = 40 -> member4

		// 디비
		// member1 = 10 -> 비회원
		// member2 = 20 -> 비회원
		// member3 = 30 -> member3
		// member4 = 40 -> member4

		List<Member> members = queryFactory
			.selectFrom(member)
			.fetch();

		for (Member member1 : members) {
			System.out.println("member1 = " + member1);
		}
	}

	@Test
	public void bulkAdd() {
		queryFactory
			.update(member)
			.set(member.age, member.age.multiply(2))
			.execute();
	}

	@Test
	public void bulkDelete() {
		queryFactory
			.delete(member)
			.where(member.age.gt(18))
			.execute();
	}

	@Test
	public void sqalFunction() {
		List<String> resultList = queryFactory
			.select(Expressions.stringTemplate(
				"function('replace', {0}, {1}, {2})",
				member.username, "member", "m"))
			.from(member)
			.fetch();

		resultList.forEach(System.out::println);
	}

	@Test
	public void sqlFunction2() {
		List<String> resultList = queryFactory
			.select(member.username)
			.from(member)
			.where(member.username
				.eq(Expressions.stringTemplate("function('upper', {0})", member.username)))
			.fetch();

		resultList.forEach(a -> {
			System.out.println(a);
		});





	}
}
