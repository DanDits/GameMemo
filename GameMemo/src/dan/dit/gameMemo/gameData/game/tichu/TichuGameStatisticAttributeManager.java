package dan.dit.gameMemo.gameData.game.tichu;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.res.Resources;
import android.os.Bundle;
import dan.dit.gameMemo.R;
import dan.dit.gameMemo.appCore.gameSetup.TeamSetupTeamsController;
import dan.dit.gameMemo.appCore.gameSetup.TeamSetupViewController;
import dan.dit.gameMemo.appCore.statistics.StatisticsActivity;
import dan.dit.gameMemo.gameData.game.Game;
import dan.dit.gameMemo.gameData.game.GameKey;
import dan.dit.gameMemo.gameData.game.GameRound;
import dan.dit.gameMemo.gameData.player.AbstractPlayerTeam;
import dan.dit.gameMemo.gameData.player.Player;
import dan.dit.gameMemo.gameData.statistics.AttributeData;
import dan.dit.gameMemo.gameData.statistics.GameStatistic;
import dan.dit.gameMemo.gameData.statistics.GameStatisticAttributeManager;
import dan.dit.gameMemo.gameData.statistics.StatisticAttribute;
import dan.dit.gameMemo.gameData.statistics.UserStatisticAttribute;

/**
 * Singleton class
 * @author Daniel
 *
 */
public class TichuGameStatisticAttributeManager extends
        GameStatisticAttributeManager {
    public static final TichuGameStatisticAttributeManager INSTANCE = new TichuGameStatisticAttributeManager();
    
    private TichuGameStatisticAttributeManager() {
        super(GameKey.TICHU);
    }
    
    protected boolean containsAllTeams(Game pGame, AttributeData data) {
        if (pGame == null) {
            throw new IllegalArgumentException("Null game to check for " + data);
        }
        AbstractPlayerTeam first = data.getTeam(0);
        AbstractPlayerTeam second = data.getTeam(1);
        TichuGame game = (TichuGame) pGame;
        return (game.getTeam1().containsTeam(first) && game.getTeam2().containsTeam(second))
                || (game.getTeam1().containsTeam(second) && game.getTeam2().containsTeam(first));
    }
    
    public static final String IDENTIFIER_ATT_ENEMY = "enemy";
    public static final String IDENTIFIER_ATT_SELF = "self";
    public static final String IDENTIFIER_ATT_PARTNER = "partner";
    public static final String IDENTIFIER_ATT_GAME_WON = "game_won";
    public static final String IDENTIFIER_ATT_GAME_LOST = "game_lost";
    public static final String IDENTIFIER_ATT_ROUND_WON = "round_won";
    public static final String IDENTIFIER_ATT_ROUND_LOST = "round_lost";
    public static final String IDENTIFIER_STAT_GAMES_PLAYED = "games_played";
    public static final String IDENTIFIER_STAT_ROUNDS_PLAYED = "round_played";
    public static final String IDENTIFIER_ATT_MERCY_RULE = "mercy_rule";
    public static final String IDENTIFIER_ATT_NON_DEFAULT_LIMIT = "non_default_limit";
    public static final String IDENTIFIER_STAT_TICHU_SCORE = "tichu_score";
    public static final String IDENTIFIER_STAT_TICHU_SCORE_SELF = "tichu_score_self";
    public static final String IDENTIFIER_STAT_CARD_SCORE = "card_score";
    public static final String IDENTIFIER_STAT_SMALL_TICHU = "small_tichu";
    public static final String IDENTIFIER_STAT_BIG_TICHU = "big_tichu";
    public static final String IDENTIFIER_ATT_SMALL_TICHU_WON = "small_tichu_won";
    public static final String IDENTIFIER_ATT_BIG_TICHU_WON = "big_tichu_won";
    public static final String IDENTIFIER_ATT_FINISHER_POS = "finisher_pos";
    public static final String IDENTIFIER_ATT_SCORE = "score_value";
        // following attributes and statistics only use native ones 
    public static final String IDENTIFIER_STAT_TICHU_SELF = "tichu_self";
    public static final String IDENTIFIER_STAT_SMALL_TICHU_PARTNER = "small_tichu_partner";
    public static final String IDENTIFIER_STAT_BIG_TICHU_PARTNER = "big_tichu_partner";
    public static final String IDENTIFIER_STAT_TICHU_PARTNER = "tichu_partner";
    public static final String IDENTIFIER_STAT_SMALL_TICHU_ENEMY = "small_tichu_enemy";
    public static final String IDENTIFIER_STAT_BIG_TICHU_ENEMY = "big_tichu_enemy";
    public static final String IDENTIFIER_STAT_TICHU_ENEMY = "tichu_enemy";
    public static final String IDENTIFIER_ATT_TICHU_WON_SELF = "att_tichu_won_self";
    public static final String IDENTIFIER_ATT_SMALL_TICHU_WON_PARTNER = "att_small_tichu_won_partner";
    public static final String IDENTIFIER_ATT_BIG_TICHU_WON_PARTNER = "att_big_tichu_won_partner";
    public static final String IDENTIFIER_ATT_TICHU_WON_PARTNER = "att_tichu_won_partner";
    public static final String IDENTIFIER_ATT_SMALL_TICHU_WON_ENEMY = "att_small_tichu_won_enemy";
    public static final String IDENTIFIER_ATT_BIG_TICHU_WON_ENEMY = "att_big_tichu_won_enemy";
    public static final String IDENTIFIER_ATT_TICHU_WON_ENEMY = "att_tichu_won_enemy";
    public static final String IDENTIFIER_STAT_SMALL_TICHU_WON_SELF = "small_tichu_won_self";
    public static final String IDENTIFIER_STAT_BIG_TICHU_WON_SELF = "big_tichu_won_self";
    public static final String IDENTIFIER_STAT_TICHU_WON_SELF = "tichu_won_self";
    public static final String IDENTIFIER_STAT_SMALL_TICHU_WON_PARTNER = "small_tichu_won_partner";
    public static final String IDENTIFIER_STAT_BIG_TICHU_WON_PARTNER = "big_tichu_won_partner";
    public static final String IDENTIFIER_STAT_TICHU_WON_PARTNER = "tichu_won_partner";
    public static final String IDENTIFIER_STAT_SMALL_TICHU_WON_ENEMY = "small_tichu_won_enemy";
    public static final String IDENTIFIER_STAT_BIG_TICHU_WON_ENEMY = "big_tichu_won_enemy";
    public static final String IDENTIFIER_STAT_TICHU_WON_ENEMY = "tichu_won_enemy"; 
    public static final String IDENTIFIER_ATT_FINISHED_FIRST_SELF = "finisher_pos_1_self";
    public static final String IDENTIFIER_ATT_FINISHED_FIRST_PARTNER = "finisher_pos_1_partner";
    public static final String IDENTIFIER_ATT_FINISHED_FIRST_SELF_OR_PARTNER = "finisher_pos_1_self_or_partner";
    public static final String IDENTIFIER_ATT_FINISHED_FIRST_ENEMY = "finisher_pos_1_enemy";
    public static final String IDENTIFIER_ATT_FINISHED_SECOND_SELF = "finisher_pos_2_self";
    public static final String IDENTIFIER_ATT_FINISHED_SECOND_PARTNER = "finisher_pos_2_partner";
    public static final String IDENTIFIER_ATT_FINISHED_SECOND_SELF_OR_PARTNER = "finisher_pos_2_self_or_partner";
    public static final String IDENTIFIER_ATT_FINISHED_SECOND_ENEMY = "finisher_pos_2_enemy";
    public static final String IDENTIFIER_ATT_FINISHED_THIRD_SELF = "finisher_pos_3_self";
    public static final String IDENTIFIER_STAT_FIRST_AND_SECOND = "finished_first_and_second";
    public static final String IDENTIFIER_STAT_200_0_WON = "finished_200_0";
    public static final String IDENTIFIER_STAT_300_0_WON = "finished_300_0";
    public static final String IDENTIFIER_STAT_400_0_WON = "finished_400_0";
    public static final String IDENTIFIER_STAT_200_0_LOST = "finished_200_0_enemy";
    public static final String IDENTIFIER_STAT_300_0_LOST = "finished_300_0_enemy";
    public static final String IDENTIFIER_STAT_400_0_LOST = "finished_400_0_enemy";
    public static final String IDENTIFIER_STAT_TOTAL_SCORE = "total_score";
    public static final String IDENTIFIER_STAT_TICHU_SCORE_PARTNER = "tichu_score_partner";
    private static final String IDENTIFIER_ATT_200_0 = "score_200_0";
    private static final String IDENTIFIER_ATT_0_200 = "score_0_200";
    private static final String IDENTIFIER_ATT_300_0 = "score_300_0";
    private static final String IDENTIFIER_ATT_0_300 = "score_0_300";
    private static final String IDENTIFIER_ATT_400_0 = "score_400_0";
    private static final String IDENTIFIER_ATT_0_400 = "score_0_400";
    
    @Override
    protected Collection<StatisticAttribute> createPredefinedAttributes() {
        Map<String, StatisticAttribute> attrs = new HashMap<String, StatisticAttribute>();
        for (StatisticAttribute generalAtt : getGeneralPredefinedAttributes()) {
            addAndCheck(generalAtt, attrs, true);
        }
        StatisticAttribute.Builder attBuilder;
        GameStatistic.Builder statBuilder;
        StatisticAttribute att;
        
        // enemy
        attBuilder = new StatisticAttribute.Builder(IDENTIFIER_ATT_ENEMY, GameKey.TICHU);
        attBuilder.setPriority(StatisticAttribute.PRIORITY_NONE);
        attBuilder.setNameAndDescriptionResId(R.string.attribute_tichu_flag_enemy_name, R.string.attribute_tichu_flag_enemy_descr);
        addAndCheck(attBuilder.getAttribute(), attrs,true);   
        
        // self
        attBuilder = new StatisticAttribute.Builder(IDENTIFIER_ATT_SELF, GameKey.TICHU);
        attBuilder.setPriority(StatisticAttribute.PRIORITY_NONE);
        attBuilder.setNameAndDescriptionResId(R.string.attribute_tichu_flag_self_name, R.string.attribute_tichu_flag_self_descr);
        addAndCheck(attBuilder.getAttribute(), attrs,true);   
        
        // partner
        attBuilder = new StatisticAttribute.Builder(IDENTIFIER_ATT_PARTNER, GameKey.TICHU);
        attBuilder.setPriority(StatisticAttribute.PRIORITY_NONE);
        attBuilder.setNameAndDescriptionResId(R.string.attribute_tichu_flag_partner_name, R.string.attribute_tichu_flag_partner_descr);
        addAndCheck(attBuilder.getAttribute(), attrs,true);  
        
        // mercy rule enabled 
        att = new UserStatisticAttribute() {
            @Override
            public boolean acceptGame(Game game, AttributeData data) {
               return super.acceptGame(game, data) && ((TichuGame) game).usesMercyRule();
            }
        };
        attBuilder = new StatisticAttribute.Builder(att, IDENTIFIER_ATT_MERCY_RULE, GameKey.TICHU);
        attBuilder.setNameAndDescriptionResId(R.string.attribute_tichu_mercy_rule_name, R.string.attribute_tichu_mercy_rule_descr);
        attBuilder.setPriority(StatisticAttribute.PRIORITY_NONE);
        addAndCheck(attBuilder.getAttribute(), attrs,true);
        
        // non default limit
        att = new UserStatisticAttribute() {
            @Override
            public boolean acceptGame(Game game, AttributeData data) {
               return super.acceptGame(game, data) && ((TichuGame) game).getScoreLimit() != TichuGame.DEFAULT_SCORE_LIMIT;
            }
        };
        attBuilder = new StatisticAttribute.Builder(att, IDENTIFIER_ATT_NON_DEFAULT_LIMIT, GameKey.TICHU);
        attBuilder.setPriority(StatisticAttribute.PRIORITY_NONE);
        attBuilder.setNameAndDescriptionResId(R.string.attribute_tichu_non_default_limit_name, R.string.attribute_tichu_non_default_limit_descr);
        addAndCheck(attBuilder.getAttribute(), attrs,true);
        
        // game won
        att = new UserStatisticAttribute() {
           
            @Override
            public boolean acceptGame(Game game, AttributeData data) {
                if (!super.acceptGame(game, data)) {
                    return false;
                }
                TichuGame g = ((TichuGame) game);
                if (g.isFinished() && containsAllTeams(g, data)) {
                    if (getTeamsCount() == 0) {
                        return true; // no team given, that means we do not care about who actually won, just if anyone won
                    }
                    boolean firstTeamOfInterest = g.getTeam1().containsTeam(getTeam(0)) && g.getTeam2().containsTeam(getTeam(1));
                    boolean firstTeamScoredMore = g.getScoreTeam1() > g.getScoreTeam2();
                    if (hasAttribute(data, IDENTIFIER_ATT_ENEMY)) {
                        // enemy attribute means that the other team is of interest
                        firstTeamOfInterest = !firstTeamOfInterest;
                    }
                    return firstTeamOfInterest ? firstTeamScoredMore : !firstTeamScoredMore;
                }
                return false;
            }
        };
        attBuilder = new StatisticAttribute.Builder(att, IDENTIFIER_ATT_GAME_WON, GameKey.TICHU);
        attBuilder.setPriority(1);
        attBuilder.setNameAndDescriptionResId(R.string.attribute_tichu_game_won_name, R.string.attribute_tichu_game_won_descr);
        addAndCheck(attBuilder.getAttribute(), attrs,true);
        
        // game lost
        attBuilder = new StatisticAttribute.Builder(IDENTIFIER_ATT_GAME_LOST, attrs.get(IDENTIFIER_ATT_GAME_WON));
        att = attBuilder.getAttribute();
        attBuilder.setNameAndDescriptionResId(R.string.attribute_tichu_game_lost_name, R.string.attribute_tichu_game_lost_descr);
        att.addAttribute(attrs.get(IDENTIFIER_ATT_ENEMY));
        addAndCheck(att, attrs,true);
        
        // round won
        att = new UserStatisticAttribute() {
            @Override
            public boolean acceptGame(Game game, AttributeData data) {
                return super.acceptGame(game, data) && containsAllTeams((TichuGame) game, data);
            }
            @Override
            public boolean acceptRound(Game game, GameRound round, AttributeData data) {
                if (!super.acceptRound(game, round, data)) {
                    return false;
                }
                if (getTeamsCount() == 0) {
                    return true; // no teams given, that means we do not care who won the round, just if anyone won it, this is true by definition of the equal score case
                }
                TichuRound r = (TichuRound) round;
                boolean mercy = ((TichuGame) game).usesMercyRule();
                boolean firstTeamOfInterest = ((TichuGame) game).getTeam1().containsTeam(getTeam(0)) && ((TichuGame) game).getTeam2().containsTeam(getTeam(1));
                if (hasAttribute(data, IDENTIFIER_ATT_ENEMY)) {
                    firstTeamOfInterest = !firstTeamOfInterest; // other team is of interest
                }
                // a team is defined to be the winner of a round if it scored more than or equal to the enemy in this round
                int scoreDiff = r.getScoreTeam1(mercy) - r.getScoreTeam2(mercy);
                return firstTeamOfInterest ? scoreDiff >= 0 : scoreDiff <= 0;
            }
        };
        attBuilder = new StatisticAttribute.Builder(att, IDENTIFIER_ATT_ROUND_WON, GameKey.TICHU);
        attBuilder.setPriority(1);
        attBuilder.setNameAndDescriptionResId(R.string.attribute_tichu_round_won_name, R.string.attribute_tichu_round_won_descr);
        addAndCheck(attBuilder.getAttribute(), attrs,true);
        
        // round lost
        attBuilder = new StatisticAttribute.Builder(IDENTIFIER_ATT_ROUND_LOST, attrs.get(GameStatisticAttributeManager.IDENTIFIER_ATT_INVERT_RESULT));
        att = attBuilder.getAttribute();
        att.addAttribute(attrs.get(IDENTIFIER_ATT_ROUND_WON));
        attBuilder.setNameAndDescriptionResId(R.string.attribute_tichu_round_lost_name, R.string.attribute_tichu_round_lost_descr);
        addAndCheck(att, attrs,true);
        
        // played games
        GameStatistic stat = new GameStatistic() {
            @Override public double calculateValue(Game gamee, AttributeData data) { return 1.0;}
            @Override public double calculateValue(Game game, GameRound rounde, AttributeData data) {return Double.NaN;}
            @Override public boolean acceptGame(Game game, AttributeData data) {return acceptGameAllSubattributes(game, data) && containsAllTeams((TichuGame) game, data);}
            @Override public boolean acceptRound(Game game, GameRound round, AttributeData data) {return false;}
        };
        statBuilder = new GameStatistic.Builder(stat, IDENTIFIER_STAT_GAMES_PLAYED, GameKey.TICHU);
        statBuilder.setPriority(2);
        statBuilder.setPresentationType(GameStatistic.PRESENTATION_TYPE_ABSOLUTE);
        statBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_games_played_name, R.string.statistic_tichu_games_played_descr);
        addAndCheck(statBuilder.getAttribute(), attrs,true);
        
        // played rounds
        stat = new GameStatistic() {
            @Override public double calculateValue(Game gamee, AttributeData data) { return Double.NaN;}
            @Override public double calculateValue(Game game, GameRound rounde, AttributeData data) {return 1.0;}
            @Override public boolean acceptGame(Game game, AttributeData data) {return acceptGameAllSubattributes(game, data) && containsAllTeams((TichuGame) game, data);}
            @Override public boolean acceptRound(Game game, GameRound round, AttributeData data) {return acceptRoundAllSubattributes(game, round, data);}
        };
        statBuilder = new GameStatistic.Builder(stat, IDENTIFIER_STAT_ROUNDS_PLAYED, GameKey.TICHU);
        statBuilder.setPriority(2);
        statBuilder.setPresentationType(GameStatistic.PRESENTATION_TYPE_ABSOLUTE);
        statBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_rounds_played_name, R.string.statistic_tichu_rounds_played_descr);
        addAndCheck(statBuilder.getAttribute(), attrs,true);
        
        // tichu score
        stat = new GameStatistic() {
            @Override public double calculateValue(Game game, AttributeData data) { return Double.NaN;}
            @Override public double calculateValue(Game game, GameRound round, AttributeData data) {
                TichuGame g = (TichuGame) game;
                TichuRound r = (TichuRound) round;
                int tichuScoreTeam1 = r.getScoreTeam1(g.usesMercyRule()) - r.getRawScoreTeam1();
                int tichuScoreTeam2 = r.getScoreTeam2(g.usesMercyRule()) - r.getRawScoreTeam2();
                if (getTeamsCount() == 0 || (hasAttribute(data, IDENTIFIER_ATT_ENEMY) && hasAttribute(data, IDENTIFIER_ATT_SELF) &&
                         hasAttribute(data, IDENTIFIER_ATT_PARTNER))) {
                    return  tichuScoreTeam1 + tichuScoreTeam2;
                } else {
                    boolean firstTeamOfInterest = ((TichuGame) game).getTeam1().containsTeam(getTeam(0)) && ((TichuGame) game).getTeam2().containsTeam(getTeam(1));
                    if (hasAttribute(data, IDENTIFIER_ATT_ENEMY)) {
                        firstTeamOfInterest = !firstTeamOfInterest; // other team is of interest
                        return firstTeamOfInterest ? tichuScoreTeam1 : tichuScoreTeam2;
                    } else if (getTeam(0) == null || getTeam(0).getPlayerCount() != 1) {
                        return firstTeamOfInterest ? tichuScoreTeam1 : tichuScoreTeam2;  // cannot make difference between self and/or partner
                    } else {
                        int playerId = g.getPlayerId(getTeam(0).getPlayers().get(0));
                        if (hasAttribute(data, IDENTIFIER_ATT_SELF)) {
                            return r.getStatisticPersonalTichuScore(g.usesMercyRule(), playerId);
                        } else if (hasAttribute(data, IDENTIFIER_ATT_PARTNER)) {
                            return r.getStatisticPersonalTichuScore(g.usesMercyRule(), TichuGame.getPartnerId(playerId));
                        } else {
                            return firstTeamOfInterest ? tichuScoreTeam1 : tichuScoreTeam2;  // default is self and partner
                        }
                    }
                }
            }
            @Override public boolean acceptGame(Game game, AttributeData data) {return acceptGameAllSubattributes(game, data) && containsAllTeams((TichuGame) game, data);}
            @Override public boolean acceptRound(Game game, GameRound round, AttributeData data) {return acceptRoundAllSubattributes(game, round, data);}
        };
        statBuilder = new GameStatistic.Builder(stat, IDENTIFIER_STAT_TICHU_SCORE, GameKey.TICHU);
        statBuilder.setPriority(48);
        statBuilder.setPresentationType(GameStatistic.PRESENTATION_TYPE_PROPORTION);
        statBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_tichu_score_name, R.string.statistic_tichu_tichu_score_descr);
        addAndCheck(statBuilder.getAttribute(), attrs,true);
        
        // own tichu score
        statBuilder = new GameStatistic.Builder(IDENTIFIER_STAT_TICHU_SCORE_SELF, (GameStatistic) attrs.get(IDENTIFIER_STAT_TICHU_SCORE));
        statBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_tichu_score_self_name, R.string.statistic_tichu_tichu_score_self_descr);
        statBuilder.getStatistic().addAttribute(attrs.get(IDENTIFIER_ATT_SELF));
        statBuilder.setPriority(46);
        addAndCheck(statBuilder.getAttribute(), attrs,true);
        
        // partner tichu score
        statBuilder = new GameStatistic.Builder(IDENTIFIER_STAT_TICHU_SCORE_PARTNER, (GameStatistic) attrs.get(IDENTIFIER_STAT_TICHU_SCORE));
        statBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_tichu_score_partner_name, R.string.statistic_tichu_tichu_score_partner_descr);
        statBuilder.getStatistic().addAttribute(attrs.get(IDENTIFIER_ATT_PARTNER));
        statBuilder.setPriority(44);
        addAndCheck(statBuilder.getAttribute(), attrs,true);
          
        // card score
        stat = new GameStatistic() {
            @Override public double calculateValue(Game game, AttributeData data) { return Double.NaN;}
            @Override public double calculateValue(Game game, GameRound round, AttributeData data) {
                TichuRound r = (TichuRound) round;
                int scoreTeam1 = r.getRawScoreTeam1();
                int scoreTeam2 = r.getRawScoreTeam2();
                if (getTeamsCount() == 0 || (hasAttribute(data, IDENTIFIER_ATT_ENEMY) && (hasAttribute(data, IDENTIFIER_ATT_SELF) ||
                        hasAttribute(data, IDENTIFIER_ATT_PARTNER)))) {
                    return  scoreTeam1 + scoreTeam2;
                } else {
                    boolean firstTeamOfInterest = ((TichuGame) game).getTeam1().containsTeam(getTeam(0)) && ((TichuGame) game).getTeam2().containsTeam(getTeam(1));
                    if (hasAttribute(data, IDENTIFIER_ATT_ENEMY)) {
                        firstTeamOfInterest = !firstTeamOfInterest; // other team is of interest
                        return firstTeamOfInterest ? scoreTeam1 : scoreTeam2;
                    } else {
                        return firstTeamOfInterest ? scoreTeam1 : scoreTeam2;
                    }
                }
            }
            @Override public boolean acceptGame(Game game, AttributeData data) {return acceptGameAllSubattributes(game, data) && containsAllTeams((TichuGame) game, data);}
            @Override public boolean acceptRound(Game game, GameRound round, AttributeData data) {return acceptRoundAllSubattributes(game, round, data);}
        };
        statBuilder = new GameStatistic.Builder(stat, IDENTIFIER_STAT_CARD_SCORE, GameKey.TICHU);
        statBuilder.setPriority(50);
        statBuilder.setPresentationType(GameStatistic.PRESENTATION_TYPE_PROPORTION);
        statBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_card_score_name, R.string.statistic_tichu_card_score_descr);
        addAndCheck(statBuilder.getAttribute(), attrs,true);
        
        // small tichu bid
        stat = new GameStatistic() {
            @Override public double calculateValue(Game game, AttributeData data) { return Double.NaN;}
            
            @Override public double calculateValue(Game game, GameRound round, AttributeData data) {
                return getTichuCount(game, round, data, TichuBidType.SMALL, false);
            }
            @Override public boolean acceptGame(Game game, AttributeData data) {return acceptGameAllSubattributes(game, data) && containsAllTeams((TichuGame) game, data);}
            @Override public boolean acceptRound(Game game, GameRound round, AttributeData data) {return acceptRoundAllSubattributes(game, round, data) && getTichuCount(game, round, data, TichuBidType.SMALL, false) > 0;}
        };
        statBuilder = new GameStatistic.Builder(stat, IDENTIFIER_STAT_SMALL_TICHU, GameKey.TICHU);
        statBuilder.setPriority(90);
        statBuilder.setPresentationType(GameStatistic.PRESENTATION_TYPE_ABSOLUTE);
        statBuilder.setReferenceStatisticIdentifier(IDENTIFIER_STAT_ROUNDS_PLAYED);
        statBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_small_tichu_name, R.string.statistic_tichu_small_tichu_descr);
        addAndCheck(statBuilder.getAttribute(), attrs,true);
        
        // big tichu bid
        stat = new GameStatistic() {
            @Override public double calculateValue(Game game, AttributeData data) { return Double.NaN;}
            
            @Override public double calculateValue(Game game, GameRound round, AttributeData data) {
                return getTichuCount(game, round, data, TichuBidType.BIG, false);
            }
            @Override public boolean acceptGame(Game game, AttributeData data) {return acceptGameAllSubattributes(game, data) && containsAllTeams((TichuGame) game, data);}
            @Override public boolean acceptRound(Game game, GameRound round, AttributeData data) {return acceptRoundAllSubattributes(game, round, data) && getTichuCount(game, round, data, TichuBidType.BIG, false) > 0;}
        };
        statBuilder = new GameStatistic.Builder(stat, IDENTIFIER_STAT_BIG_TICHU, GameKey.TICHU);
        statBuilder.setPriority(70);
        statBuilder.setPresentationType(GameStatistic.PRESENTATION_TYPE_ABSOLUTE);
        statBuilder.setReferenceStatisticIdentifier(IDENTIFIER_STAT_ROUNDS_PLAYED);
        statBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_big_tichu_name, R.string.statistic_tichu_big_tichu_descr);
        addAndCheck(statBuilder.getAttribute(), attrs,true);
        
        // small tichu won
        att = new UserStatisticAttribute() {
            @Override
            public boolean acceptRound(Game game, GameRound round, AttributeData data) {
                return super.acceptRound(game, round, data) && getTichuCount(game, round, data, TichuBidType.SMALL, true) > 0;
            }
        };
        attBuilder = new StatisticAttribute.Builder(att, IDENTIFIER_ATT_SMALL_TICHU_WON, GameKey.TICHU);
        attBuilder.setPriority(3);
        attBuilder.setNameAndDescriptionResId(R.string.attribute_tichu_small_tichu_won_name, R.string.attribute_tichu_small_tichu_won_descr);
        addAndCheck(attBuilder.getAttribute(), attrs,true);
        
        // big tichu won
        att = new UserStatisticAttribute() {
            @Override
            public boolean acceptRound(Game game, GameRound round, AttributeData data) {
                return super.acceptRound(game, round, data) && getTichuCount(game, round, data, TichuBidType.BIG, true) > 0;
            }
        };
        attBuilder = new StatisticAttribute.Builder(att, IDENTIFIER_ATT_BIG_TICHU_WON, GameKey.TICHU);
        attBuilder.setPriority(3);
        attBuilder.setNameAndDescriptionResId(R.string.attribute_tichu_big_tichu_won_name, R.string.attribute_tichu_big_tichu_won_descr);
        addAndCheck(attBuilder.getAttribute(), attrs,true);
        
        // finisher pos
        att = new UserStatisticAttribute() {
            @Override public boolean requiresCustomValue() {return true;}
            @Override public boolean acceptGame(Game game, AttributeData data) {return acceptGameAllSubattributes(game, data);}
            @Override 
            public boolean acceptRound(Game game, GameRound round, AttributeData data) {
                if (!acceptRoundAllSubattributes(game, round, data)) {
                    return false;
                }
                boolean[] relevant = getDefaultRelevantPlayers(game, round, data);
                for (int i = 0; i < relevant.length; i++) {
                    if (relevant[i] && String.valueOf(((TichuRound) round).getFinisherPos(i + TichuGame.PLAYER_ONE_ID)).equals(data.getCustomValue())) {
                        return true;
                    }
                }
                return false;
            }
        };
        attBuilder = new StatisticAttribute.Builder(att, IDENTIFIER_ATT_FINISHER_POS, GameKey.TICHU);
        attBuilder.setPriority(StatisticAttribute.PRIORITY_NONE);
        attBuilder.setCustomValue(String.valueOf(1));
        attBuilder.setNameAndDescriptionResId(R.string.attribute_tichu_finisher_pos_name, R.string.attribute_tichu_finisher_pos_descr);
        addAndCheck(attBuilder.getAttribute(), attrs,true);
       
        // score
        att = new UserStatisticAttribute() {
            
            @Override public boolean requiresCustomValue() {return true;}
            @Override public boolean acceptGame(Game game, AttributeData data) {return acceptGameAllSubattributes(game, data);}
            @Override 
            public boolean acceptRound(Game game, GameRound round, AttributeData data) {
                if (!acceptRoundAllSubattributes(game, round, data)) {
                    return false;
                }
                if (data.getCustomValue() == null) {
                    return true; // nothing to do here
                }
                String cv = data.getCustomValue();
                TichuGame g = (TichuGame) game;
                TichuRound r = (TichuRound) round;
                if (!CACHE_SCORE1.containsKey(cv)) {
                    // need to parse the custom value string and cache it
                    int score1 = -1;
                    int score2 = -1;
                    int separatorIndex = cv.indexOf(':');
                    try {
                        score1 = Integer.parseInt(cv.substring(0, separatorIndex == -1 ? cv.length() : separatorIndex));
                    } catch (NumberFormatException nfe) {
                        // nope, not a number, allow any score
                    }
                    if (separatorIndex != -1) {
                        try {
                            score2 = Integer.parseInt(cv.substring(separatorIndex + 1, cv.length()));
                        } catch (NumberFormatException nfe) {
                            // nope, not a number, allow any score
                        }
                    }
                    CACHE_SCORE1.put(cv, score1);
                    CACHE_SCORE2.put(cv, score2);
                }
                if (data.getTeamsCount() == 0) {
                    // no team given, so just check if the given score combo happened in any case
                    return checkScores(g, r, cv, true) || checkScores(g, r, cv, false);
                } else {
                    boolean firstTeamOfInterest = g.getTeam1().containsTeam(getTeam(0)) && g.getTeam2().containsTeam(getTeam(1));
                    return checkScores(g, r, cv, firstTeamOfInterest);
                }
            }
            
            private boolean checkScores(TichuGame g, TichuRound r, String cv, boolean firstTeamOfInterest) {
                int score1 = firstTeamOfInterest ? CACHE_SCORE1.get(cv) : CACHE_SCORE2.get(cv);
                int score2 = firstTeamOfInterest ? CACHE_SCORE2.get(cv) : CACHE_SCORE1.get(cv);
                return (score1 == -1 || r.getScoreTeam1(g.usesMercyRule()) == score1) && (r.getScoreTeam2(g.usesMercyRule()) == score2 || score2 == -1);
            }
        };
        attBuilder = new StatisticAttribute.Builder(att, IDENTIFIER_ATT_SCORE, GameKey.TICHU);
        attBuilder.setPriority(StatisticAttribute.PRIORITY_NONE);
        attBuilder.setCustomValue("100:?");
        attBuilder.setNameAndDescriptionResId(R.string.attribute_tichu_score_values_name, R.string.attribute_tichu_score_values_descr);
        addAndCheck(attBuilder.getAttribute(), attrs,true);
        
        // tichu self
        statBuilder = new GameStatistic.Builder(IDENTIFIER_STAT_TICHU_SELF, (GameStatistic) attrs.get(GameStatisticAttributeManager.IDENTIFIER_STAT_OR_SUMMER));
        statBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_tichu_own_name, R.string.statistic_tichu_tichu_own_descr);
        statBuilder.getStatistic().addAttribute(attrs.get(IDENTIFIER_STAT_SMALL_TICHU));
        statBuilder.getStatistic().addAttribute(attrs.get(IDENTIFIER_STAT_BIG_TICHU));
        statBuilder.setReferenceStatisticIdentifier(IDENTIFIER_STAT_ROUNDS_PLAYED);
        statBuilder.setPriority(101);
        addAndCheck(statBuilder.getAttribute(), attrs, true);
        
        // small tichu partner
        statBuilder = new GameStatistic.Builder(IDENTIFIER_STAT_SMALL_TICHU_PARTNER, (GameStatistic) attrs.get(IDENTIFIER_STAT_SMALL_TICHU));
        statBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_small_tichu_partner_name, R.string.statistic_tichu_small_tichu_partner_descr);
        statBuilder.getStatistic().addAttribute(attrs.get(IDENTIFIER_ATT_PARTNER));
        statBuilder.setReferenceStatisticIdentifier(IDENTIFIER_STAT_ROUNDS_PLAYED);
        addAndCheck(statBuilder.getAttribute(), attrs, true);
        
        // big tichu partner
        statBuilder = new GameStatistic.Builder(IDENTIFIER_STAT_BIG_TICHU_PARTNER, (GameStatistic) attrs.get(IDENTIFIER_STAT_BIG_TICHU));
        statBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_big_tichu_partner_name, R.string.statistic_tichu_big_tichu_partner_descr);
        statBuilder.getStatistic().addAttribute(attrs.get(IDENTIFIER_ATT_PARTNER));
        statBuilder.setReferenceStatisticIdentifier(IDENTIFIER_STAT_ROUNDS_PLAYED);
        addAndCheck(statBuilder.getAttribute(), attrs, true);
        
        // tichu partner
        statBuilder = new GameStatistic.Builder(IDENTIFIER_STAT_TICHU_PARTNER, (GameStatistic) attrs.get(GameStatisticAttributeManager.IDENTIFIER_STAT_OR_SUMMER));
        statBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_tichu_partner_name, R.string.statistic_tichu_tichu_partner_descr);
        statBuilder.getStatistic().addAttribute(attrs.get(IDENTIFIER_STAT_SMALL_TICHU_PARTNER));
        statBuilder.getStatistic().addAttribute(attrs.get(IDENTIFIER_STAT_BIG_TICHU_PARTNER));
        statBuilder.setReferenceStatisticIdentifier(IDENTIFIER_STAT_ROUNDS_PLAYED);
        statBuilder.setPriority(65);
        addAndCheck(statBuilder.getAttribute(), attrs, true);
        
        // small tichu enemy
        statBuilder = new GameStatistic.Builder(IDENTIFIER_STAT_SMALL_TICHU_ENEMY, (GameStatistic) attrs.get(IDENTIFIER_STAT_SMALL_TICHU));
        statBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_small_tichu_enemy_name, R.string.statistic_tichu_small_tichu_enemy_descr);
        statBuilder.getStatistic().addAttribute(attrs.get(IDENTIFIER_ATT_ENEMY));
        statBuilder.setReferenceStatisticIdentifier(IDENTIFIER_STAT_ROUNDS_PLAYED);
        addAndCheck(statBuilder.getAttribute(), attrs, true);
        
        // big tichu enemy
        statBuilder = new GameStatistic.Builder(IDENTIFIER_STAT_BIG_TICHU_ENEMY, (GameStatistic) attrs.get(IDENTIFIER_STAT_BIG_TICHU));
        statBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_big_tichu_enemy_name, R.string.statistic_tichu_big_tichu_enemy_descr);
        statBuilder.getStatistic().addAttribute(attrs.get(IDENTIFIER_ATT_ENEMY));
        statBuilder.setReferenceStatisticIdentifier(IDENTIFIER_STAT_ROUNDS_PLAYED);
        addAndCheck(statBuilder.getAttribute(), attrs, true);
        
        // tichu enemy
        statBuilder = new GameStatistic.Builder(IDENTIFIER_STAT_TICHU_ENEMY, (GameStatistic) attrs.get(GameStatisticAttributeManager.IDENTIFIER_STAT_OR_SUMMER));
        statBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_tichu_enemy_name, R.string.statistic_tichu_tichu_enemy_descr);
        statBuilder.getStatistic().addAttribute(attrs.get(IDENTIFIER_STAT_SMALL_TICHU_ENEMY));
        statBuilder.getStatistic().addAttribute(attrs.get(IDENTIFIER_STAT_BIG_TICHU_ENEMY));
        statBuilder.setReferenceStatisticIdentifier(IDENTIFIER_STAT_ROUNDS_PLAYED);
        statBuilder.setPriority(60);
        addAndCheck(statBuilder.getAttribute(), attrs, true);
        
        // own tichu won
        attBuilder = new StatisticAttribute.Builder(IDENTIFIER_ATT_TICHU_WON_SELF, attrs.get(GameStatisticAttributeManager.IDENTIFIER_ATT_OR));
        attBuilder.setPriority(-1);
        attBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_tichu_won_own_name, R.string.statistic_tichu_tichu_won_own_descr);
        attBuilder.getAttribute().addAttribute(attrs.get(IDENTIFIER_ATT_SMALL_TICHU_WON));
        attBuilder.getAttribute().addAttribute(attrs.get(IDENTIFIER_ATT_BIG_TICHU_WON));
        addAndCheck(attBuilder.getAttribute(), attrs,true);
        
        // small tichu won partner
        attBuilder = new StatisticAttribute.Builder(IDENTIFIER_ATT_SMALL_TICHU_WON_PARTNER, attrs.get(IDENTIFIER_ATT_SMALL_TICHU_WON));
        attBuilder.setPriority(-1);
        attBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_small_tichu_won_partner_name, R.string.statistic_tichu_small_tichu_won_partner_descr);
        attBuilder.getAttribute().addAttribute(attrs.get(IDENTIFIER_ATT_PARTNER));
        addAndCheck(attBuilder.getAttribute(), attrs,true);
        
        // big tichu won partner
        attBuilder = new StatisticAttribute.Builder(IDENTIFIER_ATT_BIG_TICHU_WON_PARTNER, attrs.get(IDENTIFIER_ATT_BIG_TICHU_WON));
        attBuilder.setPriority(-1);
        attBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_big_tichu_won_partner_name, R.string.statistic_tichu_big_tichu_won_partner_descr);
        attBuilder.getAttribute().addAttribute(attrs.get(IDENTIFIER_ATT_PARTNER));
        addAndCheck(attBuilder.getAttribute(), attrs,true);
        
        // tichu won partner
        attBuilder = new StatisticAttribute.Builder(IDENTIFIER_ATT_TICHU_WON_PARTNER, attrs.get(GameStatisticAttributeManager.IDENTIFIER_ATT_OR));
        attBuilder.setPriority(-1);
        attBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_tichu_won_partner_name, R.string.statistic_tichu_tichu_won_partner_descr);
        attBuilder.getAttribute().addAttribute(attrs.get(IDENTIFIER_ATT_SMALL_TICHU_WON_PARTNER));
        attBuilder.getAttribute().addAttribute(attrs.get(IDENTIFIER_ATT_BIG_TICHU_WON_PARTNER));
        addAndCheck(attBuilder.getAttribute(), attrs,true);
        
        // small tichu won enemy
        attBuilder = new StatisticAttribute.Builder(IDENTIFIER_ATT_SMALL_TICHU_WON_ENEMY, attrs.get(IDENTIFIER_ATT_SMALL_TICHU_WON));
        attBuilder.setPriority(-1);
        attBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_small_tichu_won_enemy_name, R.string.statistic_tichu_small_tichu_won_enemy_descr);
        attBuilder.getAttribute().addAttribute(attrs.get(IDENTIFIER_ATT_ENEMY));
        addAndCheck(attBuilder.getAttribute(), attrs,true);
        
        // big tichu won enemy
        attBuilder = new StatisticAttribute.Builder(IDENTIFIER_ATT_BIG_TICHU_WON_ENEMY, attrs.get(IDENTIFIER_ATT_BIG_TICHU_WON));
        attBuilder.setPriority(-1);
        attBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_big_tichu_won_enemy_name, R.string.statistic_tichu_big_tichu_won_enemy_descr);
        attBuilder.getAttribute().addAttribute(attrs.get(IDENTIFIER_ATT_ENEMY));
        addAndCheck(attBuilder.getAttribute(), attrs,true);
        
        // tichu won enemy
        attBuilder = new StatisticAttribute.Builder(IDENTIFIER_ATT_TICHU_WON_ENEMY, attrs.get(GameStatisticAttributeManager.IDENTIFIER_ATT_OR));
        attBuilder.setPriority(-1);
        attBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_tichu_won_enemy_name, R.string.statistic_tichu_tichu_won_enemy_descr);
        attBuilder.getAttribute().addAttribute(attrs.get(IDENTIFIER_ATT_SMALL_TICHU_WON_ENEMY));
        attBuilder.getAttribute().addAttribute(attrs.get(IDENTIFIER_ATT_BIG_TICHU_WON_ENEMY));
        addAndCheck(attBuilder.getAttribute(), attrs,true);
        
        // small tichu won own
        statBuilder = new GameStatistic.Builder(IDENTIFIER_STAT_SMALL_TICHU_WON_SELF, (GameStatistic) attrs.get(IDENTIFIER_STAT_SMALL_TICHU));
        statBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_small_tichu_won_own_name, R.string.statistic_tichu_small_tichu_won_own_descr);
        statBuilder.getStatistic().addAttribute(attrs.get(IDENTIFIER_ATT_SMALL_TICHU_WON));
        statBuilder.setReferenceStatisticIdentifier(IDENTIFIER_STAT_SMALL_TICHU);
        statBuilder.setPresentationType(GameStatistic.PRESENTATION_TYPE_PERCENTAGE);
        statBuilder.setPriority(88);
        addAndCheck(statBuilder.getAttribute(), attrs, true);
        
        // big tichu won own
        statBuilder = new GameStatistic.Builder(IDENTIFIER_STAT_BIG_TICHU_WON_SELF, (GameStatistic) attrs.get(IDENTIFIER_STAT_BIG_TICHU));
        statBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_big_tichu_won_own_name, R.string.statistic_tichu_big_tichu_won_own_descr);
        statBuilder.getStatistic().addAttribute(attrs.get(IDENTIFIER_ATT_BIG_TICHU_WON));
        statBuilder.setReferenceStatisticIdentifier(IDENTIFIER_STAT_BIG_TICHU);
        statBuilder.setPresentationType(GameStatistic.PRESENTATION_TYPE_PERCENTAGE);
        statBuilder.setPriority(68);
        addAndCheck(statBuilder.getAttribute(), attrs, true);
        
        // tichu won own
        statBuilder = new GameStatistic.Builder(IDENTIFIER_STAT_TICHU_WON_SELF, (GameStatistic) attrs.get(GameStatisticAttributeManager.IDENTIFIER_STAT_OR_SUMMER));
        statBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_tichu_won_own_name, R.string.statistic_tichu_tichu_won_own_descr);
        statBuilder.getStatistic().addAttribute(attrs.get(IDENTIFIER_STAT_SMALL_TICHU_WON_SELF));
        statBuilder.getStatistic().addAttribute(attrs.get(IDENTIFIER_STAT_BIG_TICHU_WON_SELF));
        statBuilder.setReferenceStatisticIdentifier(IDENTIFIER_STAT_TICHU_SELF);
        statBuilder.setPresentationType(GameStatistic.PRESENTATION_TYPE_PERCENTAGE);
        statBuilder.setPriority(100);
        addAndCheck(statBuilder.getAttribute(), attrs, true);
        
        // small tichu won partner
        statBuilder = new GameStatistic.Builder(IDENTIFIER_STAT_SMALL_TICHU_WON_PARTNER, (GameStatistic) attrs.get(IDENTIFIER_STAT_SMALL_TICHU_PARTNER));
        statBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_small_tichu_won_partner_name, R.string.statistic_tichu_small_tichu_won_partner_descr);
        statBuilder.getStatistic().addAttribute(attrs.get(IDENTIFIER_ATT_SMALL_TICHU_WON_PARTNER));
        statBuilder.setReferenceStatisticIdentifier(IDENTIFIER_STAT_SMALL_TICHU_PARTNER);
        statBuilder.setPresentationType(GameStatistic.PRESENTATION_TYPE_PERCENTAGE);
        statBuilder.setPriority(GameStatistic.PRIORITY_HIDDEN);
        addAndCheck(statBuilder.getAttribute(), attrs, true);
        
        // big tichu won partner
        statBuilder = new GameStatistic.Builder(IDENTIFIER_STAT_BIG_TICHU_WON_PARTNER, (GameStatistic) attrs.get(IDENTIFIER_STAT_BIG_TICHU_PARTNER));
        statBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_big_tichu_won_partner_name, R.string.statistic_tichu_big_tichu_won_partner_descr);
        statBuilder.getStatistic().addAttribute(attrs.get(IDENTIFIER_ATT_BIG_TICHU_WON_PARTNER));
        statBuilder.setReferenceStatisticIdentifier(IDENTIFIER_STAT_BIG_TICHU_PARTNER);
        statBuilder.setPresentationType(GameStatistic.PRESENTATION_TYPE_PERCENTAGE);
        statBuilder.setPriority(GameStatistic.PRIORITY_HIDDEN);
        addAndCheck(statBuilder.getAttribute(), attrs, true);
        
        // tichu won partner
        statBuilder = new GameStatistic.Builder(IDENTIFIER_STAT_TICHU_WON_PARTNER, (GameStatistic) attrs.get(GameStatisticAttributeManager.IDENTIFIER_STAT_OR_SUMMER));
        statBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_tichu_won_partner_name, R.string.statistic_tichu_tichu_won_partner_descr);
        statBuilder.getStatistic().addAttribute(attrs.get(IDENTIFIER_STAT_SMALL_TICHU_WON_PARTNER));
        statBuilder.getStatistic().addAttribute(attrs.get(IDENTIFIER_STAT_BIG_TICHU_WON_PARTNER));
        statBuilder.setReferenceStatisticIdentifier(IDENTIFIER_STAT_TICHU_PARTNER);
        statBuilder.setPresentationType(GameStatistic.PRESENTATION_TYPE_PERCENTAGE);
        statBuilder.setPriority(64);
        addAndCheck(statBuilder.getAttribute(), attrs, true);
        
        // small tichu won enemy
        statBuilder = new GameStatistic.Builder(IDENTIFIER_STAT_SMALL_TICHU_WON_ENEMY, (GameStatistic) attrs.get(IDENTIFIER_STAT_SMALL_TICHU_ENEMY));
        statBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_small_tichu_won_enemy_name, R.string.statistic_tichu_small_tichu_won_enemy_descr);
        statBuilder.getStatistic().addAttribute(attrs.get(IDENTIFIER_ATT_SMALL_TICHU_WON_ENEMY));
        statBuilder.setReferenceStatisticIdentifier(IDENTIFIER_STAT_SMALL_TICHU_ENEMY);
        statBuilder.setPresentationType(GameStatistic.PRESENTATION_TYPE_PERCENTAGE);
        statBuilder.setPriority(GameStatistic.PRIORITY_HIDDEN);
        addAndCheck(statBuilder.getAttribute(), attrs, true);
        
        // big tichu won enemy
        statBuilder = new GameStatistic.Builder(IDENTIFIER_STAT_BIG_TICHU_WON_ENEMY, (GameStatistic) attrs.get(IDENTIFIER_STAT_BIG_TICHU_ENEMY));
        statBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_big_tichu_won_enemy_name, R.string.statistic_tichu_big_tichu_won_enemy_descr);
        statBuilder.getStatistic().addAttribute(attrs.get(IDENTIFIER_ATT_BIG_TICHU_WON_ENEMY));
        statBuilder.setReferenceStatisticIdentifier(IDENTIFIER_STAT_BIG_TICHU_ENEMY);
        statBuilder.setPresentationType(GameStatistic.PRESENTATION_TYPE_PERCENTAGE);
        statBuilder.setPriority(GameStatistic.PRIORITY_HIDDEN);
        addAndCheck(statBuilder.getAttribute(), attrs, true);
        
        // tichu won enemy
        statBuilder = new GameStatistic.Builder(IDENTIFIER_STAT_TICHU_WON_ENEMY, (GameStatistic) attrs.get(GameStatisticAttributeManager.IDENTIFIER_STAT_OR_SUMMER));
        statBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_tichu_won_enemy_name, R.string.statistic_tichu_tichu_won_enemy_descr);
        statBuilder.getStatistic().addAttribute(attrs.get(IDENTIFIER_STAT_SMALL_TICHU_WON_ENEMY));
        statBuilder.getStatistic().addAttribute(attrs.get(IDENTIFIER_STAT_BIG_TICHU_WON_ENEMY));
        statBuilder.setReferenceStatisticIdentifier(IDENTIFIER_STAT_TICHU_ENEMY);
        statBuilder.setPriority(59);
        statBuilder.setPresentationType(GameStatistic.PRESENTATION_TYPE_PERCENTAGE);
        addAndCheck(statBuilder.getAttribute(), attrs, true);
        
        // finished first self
        attBuilder = new StatisticAttribute.Builder(IDENTIFIER_ATT_FINISHED_FIRST_SELF, attrs.get(IDENTIFIER_ATT_FINISHER_POS));
        attBuilder.setCustomValue("1");
        attBuilder.setNameAndDescriptionResId(R.string.attribute_tichu_finisher_first_self_name, R.string.attribute_tichu_finisher_first_self_descr);
        attBuilder.getAttribute().addAttribute(attrs.get(IDENTIFIER_ATT_SELF));
        addAndCheck(attBuilder.getAttribute(), attrs,true);
        
        // finished first partner
        attBuilder = new StatisticAttribute.Builder(IDENTIFIER_ATT_FINISHED_FIRST_PARTNER, attrs.get(IDENTIFIER_ATT_FINISHER_POS));
        attBuilder.setCustomValue("1");
        attBuilder.setNameAndDescriptionResId(R.string.attribute_tichu_finisher_first_partner_name, R.string.attribute_tichu_finisher_first_partner_descr);
        attBuilder.getAttribute().addAttribute(attrs.get(IDENTIFIER_ATT_PARTNER));
        addAndCheck(attBuilder.getAttribute(), attrs,true);
        
        // finished first self or partner
        attBuilder = new StatisticAttribute.Builder(IDENTIFIER_ATT_FINISHED_FIRST_SELF_OR_PARTNER, attrs.get(GameStatisticAttributeManager.IDENTIFIER_ATT_OR));
        attBuilder.setNameAndDescriptionResId(R.string.attribute_tichu_finisher_first_team_name, R.string.attribute_tichu_finisher_first_team_descr);
        attBuilder.getAttribute().addAttribute(attrs.get(IDENTIFIER_ATT_FINISHED_FIRST_SELF));
        attBuilder.getAttribute().addAttribute(attrs.get(IDENTIFIER_ATT_FINISHED_FIRST_PARTNER));
        addAndCheck(attBuilder.getAttribute(), attrs,true);
        
        // finished second self
        attBuilder = new StatisticAttribute.Builder(IDENTIFIER_ATT_FINISHED_SECOND_SELF, attrs.get(IDENTIFIER_ATT_FINISHER_POS));
        attBuilder.setNameAndDescriptionResId(R.string.attribute_tichu_finisher_second_self_name, R.string.attribute_tichu_finisher_second_self_descr);
        attBuilder.setCustomValue("2");
        attBuilder.getAttribute().addAttribute(attrs.get(IDENTIFIER_ATT_SELF));
        addAndCheck(attBuilder.getAttribute(), attrs,true);
        
        // finished second partner
        attBuilder = new StatisticAttribute.Builder(IDENTIFIER_ATT_FINISHED_SECOND_PARTNER, attrs.get(IDENTIFIER_ATT_FINISHER_POS));
        attBuilder.setNameAndDescriptionResId(R.string.attribute_tichu_finisher_second_partner_name, R.string.attribute_tichu_finisher_second_partner_descr);
        attBuilder.setCustomValue("2");
        attBuilder.getAttribute().addAttribute(attrs.get(IDENTIFIER_ATT_PARTNER));
        addAndCheck(attBuilder.getAttribute(), attrs,true);
        
        // finished second enemy
        attBuilder = new StatisticAttribute.Builder(IDENTIFIER_ATT_FINISHED_SECOND_ENEMY, attrs.get(IDENTIFIER_ATT_FINISHER_POS));
        attBuilder.setCustomValue("2");
        attBuilder.setNameAndDescriptionResId(R.string.attribute_tichu_finisher_second_enemy_name, R.string.attribute_tichu_finisher_second_enemy_descr);
        attBuilder.getAttribute().addAttribute(attrs.get(IDENTIFIER_ATT_ENEMY));
        addAndCheck(attBuilder.getAttribute(), attrs,true);
        
        // finished second self or partner
        attBuilder = new StatisticAttribute.Builder(IDENTIFIER_ATT_FINISHED_SECOND_SELF_OR_PARTNER, attrs.get(GameStatisticAttributeManager.IDENTIFIER_ATT_OR));
        attBuilder.setNameAndDescriptionResId(R.string.attribute_tichu_finisher_second_team_name, R.string.attribute_tichu_finisher_second_team_descr);
        attBuilder.getAttribute().addAttribute(attrs.get(IDENTIFIER_ATT_FINISHED_SECOND_SELF));
        attBuilder.getAttribute().addAttribute(attrs.get(IDENTIFIER_ATT_FINISHED_SECOND_PARTNER));
        addAndCheck(attBuilder.getAttribute(), attrs,true);
        
        // finished third self
        attBuilder = new StatisticAttribute.Builder(IDENTIFIER_ATT_FINISHED_THIRD_SELF, attrs.get(IDENTIFIER_ATT_FINISHER_POS));
        attBuilder.setCustomValue("3");
        attBuilder.setNameAndDescriptionResId(R.string.attribute_tichu_finisher_third_self_name, R.string.attribute_tichu_finisher_third_self_descr);
        attBuilder.getAttribute().addAttribute(attrs.get(IDENTIFIER_ATT_SELF));
        addAndCheck(attBuilder.getAttribute(), attrs,true);
        
        // 200 : 0
        attBuilder = new StatisticAttribute.Builder(IDENTIFIER_ATT_200_0, attrs.get(IDENTIFIER_ATT_SCORE));
        attBuilder.setCustomValue("200:0");
        attBuilder.setNameAndDescriptionResId(R.string.attribute_tichu_200_0_name, 0);
        addAndCheck(attBuilder.getAttribute(), attrs,true);
        
        // 0 : 200
        attBuilder = new StatisticAttribute.Builder(IDENTIFIER_ATT_0_200, attrs.get(IDENTIFIER_ATT_SCORE));
        attBuilder.setCustomValue("0:200");
        attBuilder.setNameAndDescriptionResId(R.string.attribute_tichu_0_200_name, 0);
        addAndCheck(attBuilder.getAttribute(), attrs,true); 
        
        // 300 : 0
        attBuilder = new StatisticAttribute.Builder(IDENTIFIER_ATT_300_0, attrs.get(IDENTIFIER_ATT_SCORE));
        attBuilder.setCustomValue("300:0");
        attBuilder.setNameAndDescriptionResId(R.string.attribute_tichu_300_0_name, 0);
        addAndCheck(attBuilder.getAttribute(), attrs,true);
        
        // 0 : 300
        attBuilder = new StatisticAttribute.Builder(IDENTIFIER_ATT_0_300, attrs.get(IDENTIFIER_ATT_SCORE));
        attBuilder.setCustomValue("0:300");
        attBuilder.setNameAndDescriptionResId(R.string.attribute_tichu_0_300_name, 0);
        addAndCheck(attBuilder.getAttribute(), attrs,true); 
        
        // 400 : 0
        attBuilder = new StatisticAttribute.Builder(IDENTIFIER_ATT_400_0, attrs.get(IDENTIFIER_ATT_SCORE));
        attBuilder.setCustomValue("400:0");
        attBuilder.setNameAndDescriptionResId(R.string.attribute_tichu_400_0_name, 0);
        addAndCheck(attBuilder.getAttribute(), attrs,true);
        
        // 0 : 400
        attBuilder = new StatisticAttribute.Builder(IDENTIFIER_ATT_0_400, attrs.get(IDENTIFIER_ATT_SCORE));
        attBuilder.setCustomValue("0:400");
        attBuilder.setNameAndDescriptionResId(R.string.attribute_tichu_0_400_name, 0);
        addAndCheck(attBuilder.getAttribute(), attrs,true);    
        
        // won 200:0
        statBuilder = new GameStatistic.Builder(IDENTIFIER_STAT_200_0_WON, (GameStatistic) attrs.get(IDENTIFIER_STAT_ROUNDS_PLAYED));
        statBuilder.getAttribute().addAttribute(attrs.get(IDENTIFIER_ATT_200_0));
        statBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_won_200_0_name, R.string.statistic_tichu_won_200_0_descr);
        statBuilder.setPriority(25);
        addAndCheck(statBuilder.getAttribute(), attrs,true);
        
        // lost 200:0
        statBuilder = new GameStatistic.Builder(IDENTIFIER_STAT_200_0_LOST, (GameStatistic) attrs.get(IDENTIFIER_STAT_ROUNDS_PLAYED));
        statBuilder.getAttribute().addAttribute(attrs.get(IDENTIFIER_ATT_0_200));
        statBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_won_0_200_name,R.string.statistic_tichu_won_0_200_descr);
        statBuilder.setPriority(24);
        addAndCheck(statBuilder.getAttribute(), attrs,true);
        
        // won 300:0
        statBuilder = new GameStatistic.Builder(IDENTIFIER_STAT_300_0_WON, (GameStatistic) attrs.get(IDENTIFIER_STAT_ROUNDS_PLAYED));
        statBuilder.getAttribute().addAttribute(attrs.get(IDENTIFIER_ATT_300_0));
        statBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_won_300_0_name, R.string.statistic_tichu_won_300_0_descr);
        statBuilder.setPriority(23);
        addAndCheck(statBuilder.getAttribute(), attrs,true);
        
        // lost 300:0
        statBuilder = new GameStatistic.Builder(IDENTIFIER_STAT_300_0_LOST, (GameStatistic) attrs.get(IDENTIFIER_STAT_ROUNDS_PLAYED));
        statBuilder.getAttribute().addAttribute(attrs.get(IDENTIFIER_ATT_0_300));
        statBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_won_0_300_name,R.string.statistic_tichu_won_0_300_descr);
        statBuilder.setPriority(22);
        addAndCheck(statBuilder.getAttribute(), attrs,true);
        
        // won 400:0
        statBuilder = new GameStatistic.Builder(IDENTIFIER_STAT_400_0_WON, (GameStatistic) attrs.get(IDENTIFIER_STAT_ROUNDS_PLAYED));
        statBuilder.getAttribute().addAttribute(attrs.get(IDENTIFIER_ATT_400_0));
        statBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_won_400_0_name, R.string.statistic_tichu_won_400_0_descr);
        statBuilder.setPriority(21);
        addAndCheck(statBuilder.getAttribute(), attrs,true);
        
        // lost 400:0
        statBuilder = new GameStatistic.Builder(IDENTIFIER_STAT_400_0_LOST, (GameStatistic) attrs.get(IDENTIFIER_STAT_ROUNDS_PLAYED));
        statBuilder.getAttribute().addAttribute(attrs.get(IDENTIFIER_ATT_0_400));
        statBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_won_0_400_name,R.string.statistic_tichu_won_0_400_descr);
        statBuilder.setPriority(20);
        addAndCheck(statBuilder.getAttribute(), attrs,true);
        
        // total score
        statBuilder = new GameStatistic.Builder(IDENTIFIER_STAT_TOTAL_SCORE, (GameStatistic) attrs.get(GameStatisticAttributeManager.IDENTIFIER_STAT_OR_SUMMER));
        statBuilder.getAttribute().addAttribute(attrs.get(IDENTIFIER_STAT_TICHU_SCORE_SELF));
        statBuilder.getAttribute().addAttribute(attrs.get(IDENTIFIER_STAT_CARD_SCORE));
        statBuilder.setPriority(45);
        statBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_total_score_name,R.string.statistic_tichu_total_score_descr);
        statBuilder.setReferenceStatisticIdentifier(IDENTIFIER_STAT_ROUNDS_PLAYED);
        statBuilder.setPresentationType(GameStatistic.PRESENTATION_TYPE_PROPORTION);
        addAndCheck(statBuilder.getAttribute(), attrs,true);
        return attrs.values();
    }

    private static boolean[] relevant = new boolean[TichuGame.TOTAL_PLAYERS]; // making it not thread save, but reducing allocations
    private static boolean[] getDefaultRelevantPlayers(Game game, GameRound round, AttributeData data) {
        TichuGame g = (TichuGame) game;
        if (data.getTeamsCount() == 0 || (hasAttribute(data, IDENTIFIER_ATT_ENEMY) && hasAttribute(data, IDENTIFIER_ATT_SELF) &&
                hasAttribute(data, IDENTIFIER_ATT_PARTNER))) {
            // if there is no team or all attributes are set, all players are relevant since we cannot tell friend from foe
             Arrays.fill(relevant, true);
        } else {
            Arrays.fill(relevant, false);
            boolean firstTeamOfInterest = g.getTeam1().containsTeam(data.getTeam(0)) && (data.getTeamsCount() == 1 || g.getTeam2().containsTeam(data.getTeam(1)));
            List<Player> teamOfInterest = data.getTeam(0).getPlayers();
            int playerId = TichuGame.INVALID_PLAYER_ID;
            if (teamOfInterest != null && teamOfInterest.size() == 1) {
                playerId = g.getPlayerId(teamOfInterest.get(0));
            }
            boolean noFlag = !hasAttribute(data, IDENTIFIER_ATT_ENEMY) && !hasAttribute(data, IDENTIFIER_ATT_SELF) && !hasAttribute(data, IDENTIFIER_ATT_PARTNER);
            if (playerId != TichuGame.INVALID_PLAYER_ID) {
                relevant[0] = (!firstTeamOfInterest && hasAttribute(data, IDENTIFIER_ATT_ENEMY)) || 
                        (playerId == TichuGame.PLAYER_ONE_ID && (noFlag || hasAttribute(data, IDENTIFIER_ATT_SELF))) ||
                        (playerId == TichuGame.PLAYER_TWO_ID && hasAttribute(data, IDENTIFIER_ATT_PARTNER));
                relevant[1]  = (!firstTeamOfInterest && hasAttribute(data, IDENTIFIER_ATT_ENEMY)) || 
                        (playerId == TichuGame.PLAYER_ONE_ID && hasAttribute(data, IDENTIFIER_ATT_PARTNER)) ||
                        (playerId == TichuGame.PLAYER_TWO_ID && (noFlag || hasAttribute(data, IDENTIFIER_ATT_SELF)));
                relevant[2]  = (firstTeamOfInterest && hasAttribute(data, IDENTIFIER_ATT_ENEMY)) || 
                        (playerId == TichuGame.PLAYER_THREE_ID && (noFlag || hasAttribute(data, IDENTIFIER_ATT_SELF))) ||
                        (playerId == TichuGame.PLAYER_FOUR_ID && hasAttribute(data, IDENTIFIER_ATT_PARTNER));
                relevant[3]  = (firstTeamOfInterest && hasAttribute(data, IDENTIFIER_ATT_ENEMY)) || 
                        (playerId == TichuGame.PLAYER_THREE_ID && hasAttribute(data, IDENTIFIER_ATT_PARTNER)) ||
                        (playerId == TichuGame.PLAYER_FOUR_ID && (noFlag || hasAttribute(data, IDENTIFIER_ATT_SELF)));
            } else {
                if (hasAttribute(data, IDENTIFIER_ATT_ENEMY)) {
                    if (firstTeamOfInterest) {
                        relevant[2]  = relevant[3]  = true;
                    } else {
                        relevant[0]  = relevant[1]  = true;
                    }
                } else {
                    if (firstTeamOfInterest) {
                        relevant[0]  = relevant[1]  = true;
                    } else {
                        relevant[2]  = relevant[3]  = true;
                    }
                }
            }
        }                    
        return relevant;
    }

    /* 
     * Cache of score value attribute to avoid parsing custom value strings over and over
     */
    private static final Map<String, Integer> CACHE_SCORE1 = new HashMap<String, Integer>(3);
    private static final Map<String, Integer> CACHE_SCORE2 = new HashMap<String, Integer>(3);
    /*
     * To avoid same calculation being made over and over, 
     * especially for tichus that first need to check and then count or tichus that sum small+big.
     * Cache the parameters and return cached value if excactly the same and not yet calculated
     */
    private static Game CACHE_GAME;
    private static GameRound CACHE_ROUND;
    private static TichuBidType CACHE_BID_TYPE;
    private static boolean CACHE_WON_TICHU_ONLY;
    private static AttributeData CACHE_DATA;
    private static int CACHE_RESULT;
    private static final int getTichuCount(Game game, GameRound round, AttributeData data, TichuBidType type, boolean wonTichuOnly) {
        if (game == CACHE_GAME && round == CACHE_ROUND && data == CACHE_DATA && type == CACHE_BID_TYPE && wonTichuOnly == CACHE_WON_TICHU_ONLY) {
            return CACHE_RESULT;
        }
        TichuRound r = (TichuRound) round;
        int sum = 0;
        boolean[] relevant = getDefaultRelevantPlayers(game, round, data);
        for (int i = 0; i < relevant.length; i++) {
            if (relevant[i] && isValidTichu(r, TichuGame.PLAYER_ONE_ID + i, type, wonTichuOnly)) {
                sum++;
            }
        }
        CACHE_GAME = game;
        CACHE_ROUND = round;
        CACHE_DATA = data;
        CACHE_BID_TYPE = type;
        CACHE_WON_TICHU_ONLY = wonTichuOnly;
        CACHE_RESULT = sum;
        return sum;
    }
    
    private static boolean isValidTichu(TichuRound round, int playerId, TichuBidType type, boolean wonOnly) {
        return round.getTichuBid(playerId).getType() == type && (!wonOnly || round.getTichuBid(playerId).isWon());
    }
    
    @Override
    public Bundle createTeamsParameters(int mode, Resources res, List<AbstractPlayerTeam> teamSuggestions) {
        String defaultTeamName = res.getString(dan.dit.gameMemo.R.string.statistics_team_name_player);
        String enemyTeamName = res.getString(dan.dit.gameMemo.R.string.statistics_team_name_enemy);
        TeamSetupTeamsController.Builder builder = new TeamSetupTeamsController.Builder(false, true);
        String[] teams = makeFittingNamesArray(teamSuggestions);
        if (mode == StatisticsActivity.STATISTICS_MODE_ALL) {
            builder.addTeam(2, 2, false, defaultTeamName, false, TeamSetupViewController.DEFAULT_TEAM_COLOR, true, teamNamesToArray(teamSuggestions, 0, teams));
            builder.addTeam(2, 2, false, enemyTeamName, false, TeamSetupViewController.DEFAULT_TEAM_COLOR, true, teamNamesToArray(teamSuggestions, 1, teams));
        } else {
            for (int i = 0; i < DEFAULT_STATISTIC_MAX_TEAMS; i++) {
                builder.addTeam(2, 2, i >= DEFAULT_STATISTIC_MIN_TEAMS, defaultTeamName, false, TeamSetupViewController.DEFAULT_TEAM_COLOR , true, teamNamesToArray(teamSuggestions, i, teams));
            }
        }
        return builder.build();
    }


}
