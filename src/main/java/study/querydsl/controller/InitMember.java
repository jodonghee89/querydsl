package study.querydsl.controller;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

@Profile("local")
@Component
@RequiredArgsConstructor
public class InitMember {

	private final InitMemberService initMemberService;

	@PostConstruct
	public void init() {
		initMemberService.init();
	}

	@Component
	@RequiredArgsConstructor
	static class InitMemberService {

		private final EntityManager em;

		@Transactional
		public void init() {
			Team teamA = new Team("teamA");
			Team teamB = new Team("teamB");
			em.persist(teamA);
			em.persist(teamB);

			for (int i = 0; i < 100; i++) {
				Team selectTeam = i % 2 == 0 ? teamA : teamB;
				em.persist(new Member("member" + i, i, selectTeam));
			}
		}
	}
}
