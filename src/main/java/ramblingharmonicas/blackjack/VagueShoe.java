package ramblingharmonicas.blackjack;
import ramblingharmonicas.blackjack.cards.*;

public interface VagueShoe {

void addCard(Card aCard);
Card fastDrawSpecific(CardValue myCardValue);
double fastProbabilityOf(CardValue aCardValue);
int numberOfCards();
void printContents();
}
