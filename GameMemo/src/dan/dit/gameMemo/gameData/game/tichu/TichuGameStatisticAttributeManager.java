package dan.dit.gameMemo.gameData.game.tichu;

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
    public static final int STATISTIC_MIN_TEAMS = 2;
    public static final int STATISTIC_MAX_TEAMS = 10; // >= MIN_TEAMS
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
    
    @Override
    protected Collection<StatisticAttribute> createPredefinedAttributes() {
        Map<String, StatisticAttribute> attrs = new HashMap<String, StatisticAttribute>();
        for (StatisticAttribute generalAtt : getGeneralPredefinedAttributes()) {
            addAndCheck(generalAtt, attrs);
        }
        StatisticAttribute.Builder attBuilder;
        GameStatistic.Builder statBuilder;
        StatisticAttribute att;
        
        // enemy
        attBuilder = new StatisticAttribute.Builder(IDENTIFIER_ATT_ENEMY, GameKey.TICHU);
        attBuilder.setPriority(StatisticAttribute.PRIORITY_NONE);
        attBuilder.setNameAndDescriptionResId(R.string.attribute_tichu_flag_enemy_name, R.string.attribute_tichu_flag_enemy_descr);
        addAndCheck(attBuilder.getAttribute(), attrs);   
        
        // self
        attBuilder = new StatisticAttribute.Builder(IDENTIFIER_ATT_SELF, GameKey.TICHU);
        attBuilder.setPriority(StatisticAttribute.PRIORITY_NONE);
        attBuilder.setNameAndDescriptionResId(R.string.attribute_tichu_flag_self_name, R.string.attribute_tichu_flag_self_descr);
        addAndCheck(attBuilder.getAttribute(), attrs);   
        
        // partner
        attBuilder = new StatisticAttribute.Builder(IDENTIFIER_ATT_PARTNER, GameKey.TICHU);
        attBuilder.setPriority(StatisticAttribute.PRIORITY_NONE);
        attBuilder.setNameAndDescriptionResId(R.string.attribute_tichu_flag_partner_name, R.string.attribute_tichu_flag_partner_descr);
        addAndCheck(attBuilder.getAttribute(), attrs);  
        
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
        addAndCheck(attBuilder.getAttribute(), attrs);
        
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
        addAndCheck(attBuilder.getAttribute(), attrs);
        
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
        addAndCheck(attBuilder.getAttribute(), attrs);
        
        // game lost
        attBuilder = new StatisticAttribute.Builder(IDENTIFIER_ATT_GAME_LOST, attrs.get(IDENTIFIER_ATT_GAME_WON));
        att = attBuilder.getAttribute();
        attBuilder.setNameAndDescriptionResId(R.string.attribute_tichu_game_lost_name, R.string.attribute_tichu_game_lost_descr);
        att.addAttribute(attrs.get(IDENTIFIER_ATT_ENEMY));
        addAndCheck(att, attrs);
        
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
        addAndCheck(attBuilder.getAttribute(), attrs);
        
        // round lost
        attBuilder = new StatisticAttribute.Builder(IDENTIFIER_ATT_ROUND_LOST, attrs.get(GameStatisticAttributeManager.IDENTIFIER_ATT_INVERT_RESULT));
        att = attBuilder.getAttribute();
        att.addAttribute(attrs.get(IDENTIFIER_ATT_ROUND_WON));
        attBuilder.setNameAndDescriptionResId(R.string.attribute_tichu_round_lost_name, R.string.attribute_tichu_round_lost_descr);
        addAndCheck(att, attrs);
        
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
        addAndCheck(statBuilder.getAttribute(), attrs);
        
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
        addAndCheck(statBuilder.getAttribute(), attrs);
        
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
        statBuilder.setPriority(2);
        statBuilder.setPresentationType(GameStatistic.PRESENTATION_TYPE_PROPORTION);
        statBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_tichu_score_name, R.string.statistic_tichu_tichu_score_descr);
        addAndCheck(statBuilder.getAttribute(), attrs);
        
        // own tichu score
        statBuilder = new GameStatistic.Builder(IDENTIFIER_STAT_TICHU_SCORE_SELF, (GameStatistic) attrs.get(IDENTIFIER_STAT_TICHU_SCORE));
        statBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_tichu_score_self_name, R.string.statistic_tichu_tichu_score_self_descr);
        statBuilder.getStatistic().addAttribute(attrs.get(IDENTIFIER_ATT_SELF));
        addAndCheck(statBuilder.getAttribute(), attrs);
        
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
        statBuilder.setPriority(2);
        statBuilder.setPresentationType(GameStatistic.PRESENTATION_TYPE_PROPORTION);
        statBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_card_score_name, R.string.statistic_tichu_card_score_descr);
        addAndCheck(statBuilder.getAttribute(), attrs);
        
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
        statBuilder.setPriority(3);
        statBuilder.setPresentationType(GameStatistic.PRESENTATION_TYPE_ABSOLUTE);
        statBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_small_tichu_name, R.string.statistic_tichu_small_tichu_descr);
        addAndCheck(statBuilder.getAttribute(), attrs);
        
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
        statBuilder.setPriority(3);
        statBuilder.setPresentationType(GameStatistic.PRESENTATION_TYPE_ABSOLUTE);
        statBuilder.setNameAndDescriptionResId(R.string.statistic_tichu_big_tichu_name, R.string.statistic_tichu_big_tichu_descr);
        addAndCheck(statBuilder.getAttribute(), attrs);
        
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
        addAndCheck(attBuilder.getAttribute(), attrs);
        
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
        addAndCheck(attBuilder.getAttribute(), attrs);
        
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
        addAndCheck(attBuilder.getAttribute(), attrs);
        
        return attrs.values();
    }
   
    private static boolean[] getDefaultRelevantPlayers(Game game, GameRound round, AttributeData data) {
        TichuGame g = (TichuGame) game;
        boolean[] relevant = new boolean[TichuGame.TOTAL_PLAYERS];
        if (data.getTeamsCount() == 0 || (hasAttribute(data, IDENTIFIER_ATT_ENEMY) && hasAttribute(data, IDENTIFIER_ATT_SELF) &&
                hasAttribute(data, IDENTIFIER_ATT_PARTNER))) {
            // if there is no team or all attributes are set, all players are relevant since we cannot tell friend from foe
             for (int i = 0; i < relevant.length; i++) {
                 relevant[i] = true;
             }
        } else {
            boolean firstTeamOfInterest = g.getTeam1().containsTeam(data.getTeam(0)) && g.getTeam2().containsTeam(data.getTeam(1));
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
    
    private static final int getTichuCount(Game game, GameRound round, AttributeData data, TichuBidType type, boolean wonTichuOnly) {
        TichuRound r = (TichuRound) round;
        int sum = 0;
        boolean[] relevant = getDefaultRelevantPlayers(game, round, data);
        for (int i = 0; i < relevant.length; i++) {
            if (relevant[i] && isValidTichu(r, TichuGame.PLAYER_ONE_ID + i, type, wonTichuOnly)) {
                sum++;
            }
        }
        return sum;
    }
    
    private static boolean isValidTichu(TichuRound round, int playerId, TichuBidType type, boolean wonOnly) {
        return round.getTichuBid(playerId).getType() == type && (!wonOnly || round.getTichuBid(playerId).isWon());
    }

    @Override
    public Bundle applyMode(int mode, TeamSetupTeamsController ctr, Resources res) {
        String defaultTeamName = res.getString(dan.dit.gameMemo.R.string.statistics_team_name_player);
        String enemyTeamName = res.getString(dan.dit.gameMemo.R.string.statistics_team_name_enemy);
        if (ctr == null) {
            TeamSetupTeamsController.Builder builder = new TeamSetupTeamsController.Builder(false, true);
            for (int i = 0; i < STATISTIC_MAX_TEAMS; i++) {
                builder.addTeam(2, 2, i >= STATISTIC_MIN_TEAMS, defaultTeamName, false, TeamSetupViewController.DEFAULT_TEAM_COLOR , true, null);
            }
            return builder.build();
        }
        switch(mode) {
        case StatisticsActivity.STATISTICS_MODE_ALL:
            ctr.setTeamName(1, enemyTeamName);
            break;
        case StatisticsActivity.STATISTICS_MODE_CHRONO:
            ctr.setTeamName(1, enemyTeamName);
            break;
        case StatisticsActivity.STATISTICS_MODE_OVERVIEW:
            ctr.setTeamName(1, defaultTeamName);
            break;
        }
        return null;
    }


}
