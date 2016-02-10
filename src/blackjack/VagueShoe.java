/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package blackjack;

/**
 *
 * @author Watongo
 */
public interface VagueShoe {
    void addCard(Card aCard);
    Card fastDrawSpecific (CardValue myCardValue);
    double fastProbabilityOf(CardValue aCardValue);
    int numberOfCards();
    void printContents();
}
