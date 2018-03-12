package com.google.gwt.gin.higherlower.client.gin;

import com.google.gwt.gin.higherlower.client.DeckOfCardsImageBundle;
import com.google.gwt.gin.higherlower.client.Shuffler;
import com.google.gwt.gin.higherlower.client.model.Card;
import com.google.gwt.gin.higherlower.client.model.Deck;
import com.google.gwt.gin.higherlower.client.model.Rank;
import com.google.gwt.gin.higherlower.client.model.Suit;
import com.google.inject.Inject;
import com.google.inject.Provider;

import java.util.LinkedList;

/**
 * Provides a deck of playing cards.
 */
public class DeckProvider implements Provider<Deck> {
  private final DeckOfCardsImageBundle deckImages;
  private final Shuffler shuffler;

  @Inject // Image bundles can be injected as-is!
  public DeckProvider(DeckOfCardsImageBundle deckImages, Shuffler shuffler) {
    this.deckImages = deckImages;
    this.shuffler = shuffler;
  }

  public Deck get() {
    // TODO we could probably use a generator instead of this nonsense.
    LinkedList<Card> deck = new LinkedList<Card>();
    deck.add(new Card(Suit.CLUBS, Rank.ONE,  this.deckImages.ace_clubs().createImage()));
    deck.add(new Card(Suit.CLUBS, Rank.TWO,  this.deckImages.two_clubs().createImage()));
    deck.add(new Card(Suit.CLUBS, Rank.THREE,  this.deckImages.three_clubs().createImage()));
    deck.add(new Card(Suit.CLUBS, Rank.FOUR,  this.deckImages.four_clubs().createImage()));
    deck.add(new Card(Suit.CLUBS, Rank.FIVE,  this.deckImages.five_clubs().createImage()));
    deck.add(new Card(Suit.CLUBS, Rank.SIX,  this.deckImages.six_clubs().createImage()));
    deck.add(new Card(Suit.CLUBS, Rank.SEVEN,  this.deckImages.seven_clubs().createImage()));
    deck.add(new Card(Suit.CLUBS, Rank.EIGHT,  this.deckImages.eight_clubs().createImage()));
    deck.add(new Card(Suit.CLUBS, Rank.NINE,  this.deckImages.nine_clubs().createImage()));
    deck.add(new Card(Suit.CLUBS, Rank.TEN,  this.deckImages.ten_clubs().createImage()));
    deck.add(new Card(Suit.CLUBS, Rank.JACK,  this.deckImages.jack_clubs().createImage()));
    deck.add(new Card(Suit.CLUBS, Rank.QUEEN,  this.deckImages.queen_clubs().createImage()));
    deck.add(new Card(Suit.CLUBS, Rank.KING,  this.deckImages.king_clubs().createImage()));

    deck.add(new Card(Suit.DIAMONDS, Rank.ONE,  this.deckImages.ace_diamond().createImage()));
    deck.add(new Card(Suit.DIAMONDS, Rank.TWO,  this.deckImages.two_diamond().createImage()));
    deck.add(new Card(Suit.DIAMONDS, Rank.THREE,  this.deckImages.three_diamond().createImage()));
    deck.add(new Card(Suit.DIAMONDS, Rank.FOUR,  this.deckImages.four_diamond().createImage()));
    deck.add(new Card(Suit.DIAMONDS, Rank.FIVE,  this.deckImages.five_diamond().createImage()));
    deck.add(new Card(Suit.DIAMONDS, Rank.SIX,  this.deckImages.six_diamond().createImage()));
    deck.add(new Card(Suit.DIAMONDS, Rank.SEVEN,  this.deckImages.seven_diamond().createImage()));
    deck.add(new Card(Suit.DIAMONDS, Rank.EIGHT,  this.deckImages.eight_diamond().createImage()));
    deck.add(new Card(Suit.DIAMONDS, Rank.NINE,  this.deckImages.nine_diamond().createImage()));
    deck.add(new Card(Suit.DIAMONDS, Rank.TEN,  this.deckImages.ten_diamond().createImage()));
    deck.add(new Card(Suit.DIAMONDS, Rank.JACK,  this.deckImages.jack_diamond().createImage()));
    deck.add(new Card(Suit.DIAMONDS, Rank.QUEEN,  this.deckImages.queen_diamond().createImage()));
    deck.add(new Card(Suit.DIAMONDS, Rank.KING,  this.deckImages.king_diamond().createImage()));

    deck.add(new Card(Suit.HEARTS, Rank.ONE,  this.deckImages.ace_heart().createImage()));
    deck.add(new Card(Suit.HEARTS, Rank.TWO,  this.deckImages.two_heart().createImage()));
    deck.add(new Card(Suit.HEARTS, Rank.THREE,  this.deckImages.three_heart().createImage()));
    deck.add(new Card(Suit.HEARTS, Rank.FOUR,  this.deckImages.four_heart().createImage()));
    deck.add(new Card(Suit.HEARTS, Rank.FIVE,  this.deckImages.five_heart().createImage()));
    deck.add(new Card(Suit.HEARTS, Rank.SIX,  this.deckImages.six_heart().createImage()));
    deck.add(new Card(Suit.HEARTS, Rank.SEVEN,  this.deckImages.seven_heart().createImage()));
    deck.add(new Card(Suit.HEARTS, Rank.EIGHT,  this.deckImages.eight_heart().createImage()));
    deck.add(new Card(Suit.HEARTS, Rank.NINE,  this.deckImages.nine_heart().createImage()));
    deck.add(new Card(Suit.HEARTS, Rank.TEN,  this.deckImages.ten_heart().createImage()));
    deck.add(new Card(Suit.HEARTS, Rank.JACK,  this.deckImages.jack_heart().createImage()));
    deck.add(new Card(Suit.HEARTS, Rank.QUEEN,  this.deckImages.queen_heart().createImage()));
    deck.add(new Card(Suit.HEARTS, Rank.KING,  this.deckImages.king_heart().createImage()));

    deck.add(new Card(Suit.SPADES, Rank.ONE,  this.deckImages.ace_spade().createImage()));
    deck.add(new Card(Suit.SPADES, Rank.TWO,  this.deckImages.two_spade().createImage()));
    deck.add(new Card(Suit.SPADES, Rank.THREE,  this.deckImages.three_spade().createImage()));
    deck.add(new Card(Suit.SPADES, Rank.FOUR,  this.deckImages.four_spade().createImage()));
    deck.add(new Card(Suit.SPADES, Rank.FIVE,  this.deckImages.five_spade().createImage()));
    deck.add(new Card(Suit.SPADES, Rank.SIX,  this.deckImages.six_spade().createImage()));
    deck.add(new Card(Suit.SPADES, Rank.SEVEN,  this.deckImages.seven_spade().createImage()));
    deck.add(new Card(Suit.SPADES, Rank.EIGHT,  this.deckImages.eight_spade().createImage()));
    deck.add(new Card(Suit.SPADES, Rank.NINE,  this.deckImages.nine_spade().createImage()));
    deck.add(new Card(Suit.SPADES, Rank.TEN,  this.deckImages.ten_spade().createImage()));
    deck.add(new Card(Suit.SPADES, Rank.JACK,  this.deckImages.jack_spade().createImage()));
    deck.add(new Card(Suit.SPADES, Rank.QUEEN,  this.deckImages.queen_spade().createImage()));
    deck.add(new Card(Suit.SPADES, Rank.KING,  this.deckImages.king_spade().createImage()));

    return new Deck(deck, shuffler);
  }
}
